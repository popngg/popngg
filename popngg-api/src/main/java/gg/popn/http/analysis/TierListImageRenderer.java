package gg.popn.http.analysis;

import gg.popn.application.analysis.RatingSnapshot;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults.ChartPlaydata;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults.UserPlaydata;
import gg.popn.application.user.dto.result.UserProfileResult;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

@Component
public class TierListImageRenderer {
    static final int WIDTH=1160;
    private static final int MARGIN=28,SHELL=WIDTH-MARGIN*2,LABEL_WIDTH=120,CARD_GAP=4,CARD_COLUMNS=4,CARD_HEIGHT=116;
    private static final Color BG=new Color(247,248,249),PANEL=Color.WHITE,BORDER=new Color(220,222,227),TEXT=new Color(26,28,32),MUTED=new Color(134,139,148),BRAND=new Color(247,75,75);
    private static final Color[] ACCENTS={new Color(239,68,68),new Color(249,115,22),new Color(217,70,239),new Color(139,92,246),new Color(59,130,246),new Color(6,182,212),new Color(34,197,94),new Color(234,179,8)};
    private static final String[] MEDAL_FILES={"none","gold-star","silver-star","silver-diamond","silver-circle","bronze-star","bronze-diamond","bronze-circle","black-star","black-diamond","black-circle","easy","long-off","none"};
    private static final Map<Integer,BufferedImage> MEDAL_ICONS=loadMedalIcons();
    private final HttpClient http;
    private final ExecutorService jacketExecutor=Executors.newFixedThreadPool(8);
    private final Map<String,BufferedImage> cache=Collections.synchronizedMap(new LinkedHashMap<>(256,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,BufferedImage> eldest){return size()>256;}
    });
    private final Map<String,byte[]> rendered=Collections.synchronizedMap(new LinkedHashMap<>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,byte[]> eldest){return size()>32;}
    });
    public TierListImageRenderer(){this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build());}
    TierListImageRenderer(HttpClient http){this.http=http;}

    public byte[] render(RatingSnapshot snapshot,UserPlaydata user,UserProfileResult profile,int level,RatingController.Metric metric)throws IOException {
        String renderKey=snapshot.snapshotId()+":"+user.poptomoId()+":"+profile.updatedAt()+":"+user.playdata().hashCode()+":"+level+":"+metric;var ready=rendered.get(renderKey);if(ready!=null)return ready;
        var records=new HashMap<Long,ChartPlaydata>();user.playdata().forEach(r->records.put(r.chartId(),r));
        var eligible=snapshot.charts().stream().filter(c->c.level()==level).filter(c->"ELIGIBLE".equals(status(c,metric)))
                .sorted(metric.comparator()).toList();
        var held=snapshot.charts().stream().filter(c->c.level()==level).filter(c->!"ELIGIBLE".equals(status(c,metric))).toList();
        var bands=bands(eligible,metric);
        var all=new ArrayList<ChartRating>(eligible);all.addAll(held);
        var jackets=new ConcurrentHashMap<Long,BufferedImage>();
        var loads=all.stream().map(c->CompletableFuture.runAsync(()->{var loaded=jacket(c.jacketUrl());if(loaded!=null)jackets.put(c.chartId(),loaded);},jacketExecutor)).toList();
        loads.forEach(CompletableFuture::join);
        var avatar=remoteImage(profile.profileImageUrl());
        int height=MARGIN+headerHeight()+58+bands.stream().mapToInt(b->bandHeight(b.size())).sum()+(held.isEmpty()?0:bandHeight(held.size())+14)+55;
        var image=new BufferedImage(WIDTH,height,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();quality(g);
        paintBackground(g,height);int y=MARGIN;drawHeader(g,snapshot,user,profile,avatar,level,metric,eligible,records,y);y+=headerHeight();
        drawToolbar(g,bands.size(),y);y+=58;
        int index=0;for(var band:bands){int h=bandHeight(band.size());drawBand(g,"P-"+String.format("%02d",++index),band,y,h,ACCENTS[(index-1)%ACCENTS.length],records,jackets,level,metric);y+=h;}
        if(!held.isEmpty()){y+=14;int h=bandHeight(held.size());drawBand(g,"HOLD",held,y,h,new Color(141,113,142),records,jackets,level,metric);y+=h;}
        drawFooter(g,level,metric,y+18);g.dispose();
        try(var out=new ByteArrayOutputStream()){if(!ImageIO.write(image,"png",out))throw new IOException("PNG_WRITER_UNAVAILABLE");byte[] png=out.toByteArray();rendered.put(renderKey,png);return png;}
    }
    @PreDestroy void shutdown(){jacketExecutor.shutdown();}

    static List<List<ChartRating>> bands(List<ChartRating> charts,RatingController.Metric metric){
        if(charts.isEmpty())return List.of();if(charts.size()==1)return List.of(charts);
        var gaps=new ArrayList<Double>();for(int i=0;i<charts.size()-1;i++)gaps.add(Math.abs(value(charts.get(i),metric)-value(charts.get(i+1),metric)));
        var sorted=new ArrayList<>(gaps);Collections.sort(sorted);double median=quantile(sorted,.5),q1=quantile(sorted,.25),q3=quantile(sorted,.75);
        var deviations=sorted.stream().map(v->Math.abs(v-median)).sorted().toList();double mad=quantile(deviations,.5);
        double threshold=Math.max(median+4*Math.max(mad,1e-9),q3+1.5*Math.max(q3-q1,1e-9));
        var rankedCandidates=new ArrayList<Integer>();for(int i=0;i<gaps.size();i++)if(gaps.get(i)>threshold)rankedCandidates.add(i);
        rankedCandidates.sort(Comparator.comparingDouble((Integer i)->gaps.get(i)).reversed());
        var candidates=new ArrayList<Integer>();
        for(int candidate:rankedCandidates){
            int boundary=candidate+1;
            if(boundary<3 || charts.size()-boundary<3)continue;
            if(candidates.stream().map(i->i+1).allMatch(existing->Math.abs(existing-boundary)>=3))candidates.add(candidate);
            if(candidates.size()==7)break;
        }
        Collections.sort(candidates);
        var breaks=new HashSet<>(candidates);var result=new ArrayList<List<ChartRating>>();int start=0;
        for(int i=0;i<charts.size()-1;i++)if(breaks.contains(i)){result.add(charts.subList(start,i+1));start=i+1;}
        result.add(charts.subList(start,charts.size()));return result;
    }
    private static double quantile(List<Double> values,double q){double p=(values.size()-1)*q;int lo=(int)p,hi=(int)Math.ceil(p);return values.get(lo)+(values.get(hi)-values.get(lo))*(p-lo);}
    private static double value(ChartRating c,RatingController.Metric metric){return metric==RatingController.Metric.CPI?c.cpi():c.spi();}
    private static String status(ChartRating c,RatingController.Metric metric){return metric==RatingController.Metric.CPI?c.cpiEligibilityStatus():c.spiEligibilityStatus();}
    private static int headerHeight(){return 244;}
    private static int bandHeight(int songs){return 18+Math.max(1,(songs+CARD_COLUMNS-1)/CARD_COLUMNS)*(CARD_HEIGHT+CARD_GAP)+14;}

    private void drawHeader(Graphics2D g,RatingSnapshot snapshot,UserPlaydata user,UserProfileResult profile,BufferedImage avatar,int level,RatingController.Metric metric,List<ChartRating> charts,Map<Long,ChartPlaydata> records,int y){
        fill(g,PANEL,MARGIN,y,SHELL,244);stroke(g,BORDER,MARGIN,y,SHELL,244);
        drawAvatar(g,avatar,MARGIN+22,y+26,94);font(g,Font.BOLD,34);g.setColor(TEXT);g.drawString(fit(g,user.userName(),500),MARGIN+142,y+62);
        roundedFill(g,new Color(243,244,245),MARGIN+142,y+75,178,29,6);font(g,Font.PLAIN,12);g.setColor(new Color(85,93,109));center(g,"#  "+user.poptomoId(),MARGIN+142,y+75,178,29);
        font(g,Font.BOLD,13);g.setColor(new Color(207,49,49));g.drawString("♥  "+valueOrDash(profile.characterName()),MARGIN+142,y+130);
        g.setColor(new Color(229,231,235));g.fillRect(MARGIN+22,y+157,3,27);font(g,Font.PLAIN,13);g.setColor(new Color(85,93,109));g.drawString(fit(g,valueOrDash(profile.comment()),610),MARGIN+38,y+176);
        int sx=MARGIN+SHELL-340;drawProfileStats(g,profile,sx,y+28,310);
        g.setColor(BORDER);g.drawLine(MARGIN,y+198,MARGIN+SHELL,y+198);
        long played=charts.stream().filter(c->records.containsKey(c.chartId())).count();long clear=charts.stream().filter(c->{var r=records.get(c.chartId());return r!=null&&isCleared(r.medal().code());}).count();
        stat(g,"PLAYED",played+" / "+charts.size(),MARGIN+22,y+227);stat(g,"CLEAR",clear+" / "+played+(played==0?"":"  "+String.format("%.1f%%",100d*clear/played)),MARGIN+190,y+227);
        fill(g,new Color(255,224,224),MARGIN+SHELL-92,y+207,67,27);font(g,Font.BOLD,11);g.setColor(new Color(124,30,28));center(g,metric.name(),MARGIN+SHELL-92,y+207,67,27);
        font(g,Font.BOLD,9);g.setColor(new Color(134,139,148));g.drawString(snapshot.modelStatus()+"   •   "+snapshot.publicationStatus(),MARGIN+SHELL-330,y+225);
    }
    private static void drawProfileStats(Graphics2D g,UserProfileResult profile,int x,int y,int width){
        font(g,Font.PLAIN,11);g.setColor(MUTED);g.drawString("팝픈클래스",x,y+8);font(g,Font.BOLD,28);g.setColor(TEXT);String current=String.format("%.2f",profile.displayPopclass()/1000d);g.drawString(current,x,y+43);int currentWidth=g.getFontMetrics().stringWidth(current);
        font(g,Font.PLAIN,12);g.setColor(new Color(85,93,109));g.drawString("/ 구"+String.format("%.2f",profile.legacyPopclass()/100d),x+currentWidth+9,y+42);g.setColor(BORDER);g.drawLine(x,y+61,x+width,y+61);
        var summaries=new HashMap<String,UserProfileResult.MedalSummary>();profile.medalSummaries().forEach(s->summaries.put(s.kind(),s));
        profileStat(g,"클리어",level(summaries,"clear"),new Color(255,77,128),x,y+86,width);
        profileStat(g,"풀콤보",level(summaries,"full-combo"),new Color(50,203,204),x,y+116,width);
        profileStat(g,"퍼펙트",level(summaries,"perfect"),new Color(255,191,0),x,y+146,width);
    }
    private static void profileStat(Graphics2D g,String label,int level,Color color,int x,int y,int width){g.setColor(color);g.fillOval(x,y-8,7,7);font(g,Font.BOLD,12);g.drawString(label,x+15,y);font(g,Font.PLAIN,15);g.setColor(TEXT);String value=level==0?"—":level+"";g.drawString(value,x+width-31-g.getFontMetrics().stringWidth(value),y);font(g,Font.PLAIN,10);g.setColor(MUTED);g.drawString("Lv",x+width-25,y);}
    private static int level(Map<String,UserProfileResult.MedalSummary> summaries,String kind){var summary=summaries.get(kind);return summary==null?0:summary.maxLevel();}
    private static String valueOrDash(String value){return value==null||value.isBlank()?"—":value;}
    private static void stat(Graphics2D g,String label,String value,int x,int y){font(g,Font.BOLD,9);g.setColor(MUTED);g.drawString(label,x,y);font(g,Font.BOLD,14);g.setColor(TEXT);g.drawString(value,x+58,y);}
    private static void drawToolbar(Graphics2D g,int partitions,int y){font(g,Font.BOLD,10);g.setColor(new Color(85,93,109));g.drawString("GAP PARTITIONS  "+partitions,MARGIN+4,y+21);font(g,Font.PLAIN,10);g.setColor(MUTED);g.drawString("score / medal overlay enabled",WIDTH-MARGIN-175,y+21);font(g,Font.BOLD,9);g.setColor(new Color(134,139,148));g.drawString("FEATURE MARKS",MARGIN+4,y+45);drawLegendItem(g,"個","개인차",new Color(75,123,229),MARGIN+96,y+32);drawLegendItem(g,"辛G","짠게",new Color(234,96,104),MARGIN+160,y+32);drawLegendItem(g,"辛判","짠판",new Color(247,196,81),MARGIN+217,y+32);drawLegendItem(g,"EX","EXTRA",new Color(139,92,246),MARGIN+279,y+32);drawLegendItem(g,"超EX","초EXTRA",new Color(91,33,182),MARGIN+347,y+32);}
    private void drawBand(Graphics2D g,String label,List<ChartRating> charts,int y,int height,Color accent,Map<Long,ChartPlaydata> records,Map<Long,BufferedImage> jackets,int level,RatingController.Metric metric){
        fill(g,Color.WHITE,MARGIN,y,SHELL,height);stroke(g,BORDER,MARGIN,y,SHELL,height);fill(g,mix(Color.WHITE,accent,.09),MARGIN,y,LABEL_WIDTH,height);fill(g,accent,MARGIN,y,3,height);
        font(g,Font.BOLD,16);g.setColor(new Color(42,48,56));g.drawString(label,MARGIN+15,y+35);font(g,Font.PLAIN,10);g.setColor(new Color(134,139,148));g.drawString(charts.size()+" charts",MARGIN+15,y+53);
        int areaX=MARGIN+LABEL_WIDTH+10,cardW=(SHELL-LABEL_WIDTH-20-CARD_GAP*(CARD_COLUMNS-1))/CARD_COLUMNS;
        for(int i=0;i<charts.size();i++){int col=i%CARD_COLUMNS,row=i/CARD_COLUMNS,x=areaX+col*(cardW+CARD_GAP),cy=y+10+row*(CARD_HEIGHT+CARD_GAP);drawCard(g,charts.get(i),records.get(charts.get(i).chartId()),jackets.get(charts.get(i).chartId()),x,cy,cardW,accent,level,metric);}
    }
    private static void drawCard(Graphics2D g,ChartRating chart,ChartPlaydata record,BufferedImage jacket,int x,int y,int w,Color accent,int level,RatingController.Metric metric){
        fill(g,Color.WHITE,x,y,w,CARD_HEIGHT);fill(g,accent,x,y,w,3);stroke(g,new Color(209,211,216),x,y,w,CARD_HEIGHT);
        if(jacket!=null)cover(g,jacket,x+1,y+3,w-2,62);else{fill(g,new Color(238,239,241),x+1,y+3,w-2,62);font(g,Font.BOLD,10);g.setColor(new Color(176,179,186));center(g,"popn.gg",x+1,y+3,w-2,62);}
        drawFeatureBadges(g,chart,metric,x+6,y+3);
        fill(g,new Color(250,251,253),x+1,y+65,w-2,23);fill(g,accent,x+7,y+70,34,13);font(g,Font.BOLD,8);g.setColor(Color.WHITE);center(g,difficulty(chart.difficulty())+" "+level,x+7,y+70,34,13);
        font(g,Font.BOLD,11);g.setColor(new Color(36,43,56));g.drawString(fit(g,chart.songName(),w-54),x+47,y+81);
        g.setColor(new Color(229,232,238));g.drawLine(x+1,y+88,x+w-1,y+88);font(g,Font.BOLD,9);g.setColor(new Color(85,93,109));g.drawString(metricValue(chart,metric),x+7,y+106);
        if(record==null){font(g,Font.BOLD,10);g.setColor(new Color(154,163,178));g.drawString("NO PLAY",x+w-52,y+106);return;}
        drawMedal(g,record.medal().code(),x+w-82,y+94);font(g,Font.BOLD,11);g.setColor(new Color(20,26,38));String score=String.format("%,d",record.allTimeBest().score());g.drawString(score,x+w-7-g.getFontMetrics().stringWidth(score),y+106);
    }
    private static String metricValue(ChartRating chart,RatingController.Metric metric){Double value=metric==RatingController.Metric.CPI?chart.cpi():chart.spi();if(value==null)return metric.name()+" —";return metric==RatingController.Metric.CPI?String.format("CPI %+.2f",value):String.format("SPI %,.0f",value);}
    private static void drawFeatureBadges(Graphics2D g,ChartRating chart,RatingController.Metric metric,int x,int y){
        var features=new ArrayList<Feature>();String individuality=metric==RatingController.Metric.CPI?chart.cpiIndividualityStatus():chart.spiIndividualityStatus();
        if("CANDIDATE".equals(individuality)||"CONFIRMED".equals(individuality))features.add(new Feature("個",25,new Color(75,123,229),new Color(50,103,214),new Color(40,84,184),new Color(120,162,255),Color.WHITE));
        if(chart.strictGauge())features.add(new Feature("辛G",25,new Color(234,96,104),new Color(217,74,82),new Color(185,54,64),new Color(255,133,139),Color.WHITE));
        if(chart.strictJudgement())features.add(new Feature("辛判",29,new Color(247,196,81),new Color(239,174,50),new Color(213,138,24),new Color(255,217,120),new Color(33,23,6)));
        if("EXTRA".equals(chart.extraType()))features.add(new Feature("EX",25,new Color(167,139,250),new Color(139,92,246),new Color(109,40,217),new Color(196,181,253),Color.WHITE));
        if("SUPER_EXTRA".equals(chart.extraType()))features.add(new Feature("超EX",31,new Color(124,58,237),new Color(91,33,182),new Color(76,29,149),new Color(167,139,250),Color.WHITE));
        int offset=0;for(var feature:features){drawFeatureBadge(g,feature,x+offset,y);offset+=feature.width()+3;}
    }
    private static void drawFeatureBadge(Graphics2D g,Feature feature,int x,int y){
        int w=feature.width(),h=31;var shape=new Path2D.Double();shape.moveTo(x,y);shape.lineTo(x+w,y);shape.lineTo(x+w,y+24);shape.lineTo(x+w/2.0,y+h);shape.lineTo(x,y+24);shape.closePath();
        var paint=g.getPaint();g.setPaint(new LinearGradientPaint(x,y,x,y+h,new float[]{0,.48f,1},new Color[]{feature.highlight(),feature.main(),feature.dark()}));g.fill(shape);g.setPaint(paint);g.setColor(feature.border());g.draw(shape);font(g,Font.BOLD,10);g.setColor(feature.text());center(g,feature.mark(),x,y-1,w,25);
    }
    private static void drawLegendBadge(Graphics2D g,String mark,Color color,int x,int y){fill(g,color,x,y,14,14);font(g,Font.BOLD,8);g.setColor(Color.WHITE);center(g,mark,x,y,14,14);}
    private static void drawLegendItem(Graphics2D g,String mark,String label,Color color,int x,int y){drawLegendBadge(g,mark,color,x,y);font(g,Font.BOLD,9);g.setColor(MUTED);g.drawString(label,x+18,y+13);}
    private static void drawMedal(Graphics2D g,int code,int x,int y){g.drawImage(MEDAL_ICONS.getOrDefault(code,MEDAL_ICONS.get(13)),x-1,y-1,16,16,null);}
    static String medalIconResource(int code){int normalized=code>=1&&code<=13?code:13;return "/medals/"+MEDAL_FILES[normalized]+".png";}
    private static Map<Integer,BufferedImage> loadMedalIcons(){
        var result=new HashMap<Integer,BufferedImage>();
        for(int code=1;code<=13;code++)try(var in=TierListImageRenderer.class.getResourceAsStream(medalIconResource(code))){
            if(in==null)throw new IllegalStateException("Missing medal icon: "+medalIconResource(code));
            var image=ImageIO.read(in);if(image==null)throw new IllegalStateException("Invalid medal icon: "+medalIconResource(code));result.put(code,image);
        }catch(IOException exception){throw new UncheckedIOException(exception);}
        return Map.copyOf(result);
    }
    static boolean isCleared(int code){return code>=1&&code<=7 || code==11 || code==12;}
    private static String difficulty(int code){return switch(code){case 1->"E";case 2->"N";case 3->"H";case 4->"EX";default->"?";};}
    private static void drawFooter(Graphics2D g,int level,RatingController.Metric metric,int y){font(g,Font.PLAIN,10);g.setColor(new Color(134,139,148));g.drawString("popn.gg / personal tier map",MARGIN+3,y);String right="Lv"+level+" · "+metric+" · EXPERIMENTAL";g.drawString(right,WIDTH-MARGIN-g.getFontMetrics().stringWidth(right),y);}

    private BufferedImage jacket(String url){return remoteImage(url);}
    private BufferedImage remoteImage(String url){if(url==null||url.isBlank())return null;var cached=cache.get(url);if(cached!=null)return cached;try{var uri=URI.create(url);if(!"https".equals(uri.getScheme())||!"static.popn.gg".equalsIgnoreCase(uri.getHost()))return null;var response=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).header("Accept","image/*").GET().build(),HttpResponse.BodyHandlers.ofByteArray());if(response.statusCode()!=200||response.body().length>2_000_000)return null;var image=ImageIO.read(new ByteArrayInputStream(response.body()));if(image!=null)cache.put(url,image);return image;}catch(Exception ignored){return null;}}
    private static void drawAvatar(Graphics2D g,BufferedImage avatar,int x,int y,int size){var old=g.getClip();var shape=new RoundRectangle2D.Double(x,y,size,size,24,24);g.setClip(shape);if(avatar==null){fill(g,new Color(243,244,245),x,y,size,size);font(g,Font.BOLD,22);g.setColor(new Color(134,139,148));center(g,"popn",x,y,size,size);}else cover(g,avatar,x,y,size,size);g.setClip(old);g.setColor(new Color(229,231,235));g.draw(shape);}
    private static void cover(Graphics2D g,BufferedImage image,int x,int y,int w,int h){double scale=Math.max((double)w/image.getWidth(),(double)h/image.getHeight());int sw=(int)Math.round(w/scale),sh=(int)Math.round(h/scale),sx=(image.getWidth()-sw)/2,sy=(image.getHeight()-sh)/2;g.drawImage(image,x,y,x+w,y+h,sx,sy,sx+sw,sy+sh,null);}
    private static void paintBackground(Graphics2D g,int height){g.setColor(BG);g.fillRect(0,0,WIDTH,height);g.setPaint(new GradientPaint(0,0,new Color(255,240,240,150),0,360,new Color(247,248,249,0)));g.fillRect(0,0,WIDTH,360);}
    private static void quality(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);}
    private static void font(Graphics2D g,int style,int size){g.setFont(new Font("Noto Sans CJK JP",style,size));}
    private static void fill(Graphics2D g,Color c,int x,int y,int w,int h){g.setColor(c);g.fillRect(x,y,w,h);}
    private static void roundedFill(Graphics2D g,Color c,int x,int y,int w,int h,int radius){g.setColor(c);g.fillRoundRect(x,y,w,h,radius,radius);}
    private static void stroke(Graphics2D g,Color c,int x,int y,int w,int h){g.setColor(c);g.drawRect(x,y,w-1,h-1);}
    private static void center(Graphics2D g,String text,int x,int y,int w,int h){var fm=g.getFontMetrics();g.drawString(text,x+(w-fm.stringWidth(text))/2,y+(h-fm.getHeight())/2+fm.getAscent());}
    private static String fit(Graphics2D g,String value,int width){if(value==null)return "";var fm=g.getFontMetrics();if(fm.stringWidth(value)<=width)return value;String ellipsis="…";int end=value.length();while(end>0&&fm.stringWidth(value.substring(0,end)+ellipsis)>width)end--;return value.substring(0,end)+ellipsis;}
    private static Color mix(Color base,Color tint,double amount){return new Color((int)Math.round(base.getRed()*(1-amount)+tint.getRed()*amount),(int)Math.round(base.getGreen()*(1-amount)+tint.getGreen()*amount),(int)Math.round(base.getBlue()*(1-amount)+tint.getBlue()*amount));}
    private record Feature(String mark,int width,Color highlight,Color main,Color dark,Color border,Color text){}
}
