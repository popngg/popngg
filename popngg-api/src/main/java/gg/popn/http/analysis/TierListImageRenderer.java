package gg.popn.http.analysis;

import gg.popn.application.analysis.RatingSnapshot;
import gg.popn.application.analysis.RatingSnapshot.ChartRating;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults.ChartPlaydata;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults.UserPlaydata;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

@Component
public class TierListImageRenderer {
    static final int WIDTH=1160;
    private static final int MARGIN=28,SHELL=WIDTH-MARGIN*2,LABEL_WIDTH=120,CARD_GAP=4,CARD_COLUMNS=4,CARD_HEIGHT=116;
    private static final Color BG=new Color(9,11,16),PANEL=new Color(17,20,27),BORDER=new Color(43,49,61),TEXT=new Color(240,243,250),MUTED=new Color(115,126,145);
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

    public byte[] render(RatingSnapshot snapshot,UserPlaydata user,int level,RatingController.Metric metric)throws IOException {
        String renderKey=snapshot.snapshotId()+":"+user.poptomoId()+":"+level+":"+metric;var ready=rendered.get(renderKey);if(ready!=null)return ready;
        var records=new HashMap<Long,ChartPlaydata>();user.playdata().forEach(r->records.put(r.chartId(),r));
        var eligible=snapshot.charts().stream().filter(c->c.level()==level).filter(c->"ELIGIBLE".equals(status(c,metric)))
                .sorted(metric.comparator()).toList();
        var held=snapshot.charts().stream().filter(c->c.level()==level).filter(c->!"ELIGIBLE".equals(status(c,metric))).toList();
        var bands=bands(eligible,metric);
        var all=new ArrayList<ChartRating>(eligible);all.addAll(held);
        var jackets=new ConcurrentHashMap<Long,BufferedImage>();
        var loads=all.stream().map(c->CompletableFuture.runAsync(()->{var loaded=jacket(c.jacketUrl());if(loaded!=null)jackets.put(c.chartId(),loaded);},jacketExecutor)).toList();
        loads.forEach(CompletableFuture::join);
        int height=MARGIN+headerHeight()+36+bands.stream().mapToInt(b->bandHeight(b.size())).sum()+(held.isEmpty()?0:bandHeight(held.size())+14)+55;
        var image=new BufferedImage(WIDTH,height,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();quality(g);
        paintBackground(g,height);int y=MARGIN;drawHeader(g,snapshot,user,level,metric,eligible,records,y);y+=headerHeight();
        drawToolbar(g,bands.size(),y);y+=36;
        int index=0;for(var band:bands){int h=bandHeight(band.size());drawBand(g,"P-"+String.format("%02d",++index),band,y,h,ACCENTS[(index-1)%ACCENTS.length],records,jackets,level);y+=h;}
        if(!held.isEmpty()){y+=14;int h=bandHeight(held.size());drawBand(g,"HOLD",held,y,h,new Color(141,113,142),records,jackets,level);y+=h;}
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
    private static int headerHeight(){return 154;}
    private static int bandHeight(int songs){return 18+Math.max(1,(songs+CARD_COLUMNS-1)/CARD_COLUMNS)*(CARD_HEIGHT+CARD_GAP)+14;}

    private void drawHeader(Graphics2D g,RatingSnapshot snapshot,UserPlaydata user,int level,RatingController.Metric metric,List<ChartRating> charts,Map<Long,ChartPlaydata> records,int y){
        fill(g,PANEL,MARGIN,y,SHELL,154);stroke(g,BORDER,MARGIN,y,SHELL,154);font(g,Font.BOLD,11);g.setColor(new Color(137,147,166));g.drawString("●  popn.gg   /   PERSONAL MAP",MARGIN+18,y+25);
        font(g,Font.BOLD,34);g.setColor(new Color(245,247,251));g.drawString(fit(g,user.userName(),690),MARGIN+18,y+73);font(g,Font.PLAIN,34);g.setColor(new Color(119,129,152));g.drawString("· Lv"+level,MARGIN+30+g.getFontMetrics(new Font("SansSerif",Font.BOLD,34)).stringWidth(fitWith(new Font("SansSerif",Font.BOLD,34),user.userName(),690)),y+73);
        font(g,Font.PLAIN,12);g.setColor(MUTED);g.drawString("poptomoId "+user.poptomoId()+"   •   generated "+LocalDate.now(ZoneId.of("Asia/Seoul")),MARGIN+18,y+98);
        fill(g,new Color(228,232,255),MARGIN+SHELL-95,y+45,70,32);font(g,Font.BOLD,12);g.setColor(new Color(21,26,39));center(g,metric.name(),MARGIN+SHELL-95,y+45,70,32);
        g.setColor(BORDER);g.drawLine(MARGIN,y+112,MARGIN+SHELL,y+112);
        long played=charts.stream().filter(c->records.containsKey(c.chartId())).count();long clear=charts.stream().filter(c->{var r=records.get(c.chartId());return r!=null&&isCleared(r.medal().code());}).count();
        stat(g,"PLAYED",played+" / "+charts.size(),MARGIN+18,y+139);stat(g,"CLEAR",clear+(played==0?"":"  "+String.format("%.1f%%",100d*clear/played)),MARGIN+180,y+139);
        font(g,Font.BOLD,10);g.setColor(new Color(123,135,155));g.drawString(snapshot.modelStatus()+"   •   "+snapshot.publicationStatus(),MARGIN+SHELL-245,y+141);
    }
    private static void stat(Graphics2D g,String label,String value,int x,int y){font(g,Font.BOLD,9);g.setColor(MUTED);g.drawString(label,x,y);font(g,Font.BOLD,14);g.setColor(TEXT);g.drawString(value,x+58,y);}
    private static void drawToolbar(Graphics2D g,int partitions,int y){font(g,Font.BOLD,10);g.setColor(new Color(105,116,137));g.drawString("GAP PARTITIONS  "+partitions,MARGIN+4,y+23);font(g,Font.PLAIN,10);g.drawString("score / medal overlay enabled",WIDTH-MARGIN-175,y+23);}
    private void drawBand(Graphics2D g,String label,List<ChartRating> charts,int y,int height,Color accent,Map<Long,ChartPlaydata> records,Map<Long,BufferedImage> jackets,int level){
        fill(g,new Color(16,19,26),MARGIN,y,SHELL,height);stroke(g,BORDER,MARGIN,y,SHELL,height);fill(g,new Color(accent.getRed()/8+14,accent.getGreen()/8+16,accent.getBlue()/8+20),MARGIN,y,LABEL_WIDTH,height);fill(g,accent,MARGIN,y,3,height);
        font(g,Font.BOLD,16);g.setColor(new Color(233,237,246));g.drawString(label,MARGIN+15,y+35);font(g,Font.PLAIN,10);g.setColor(new Color(102,114,135));g.drawString(charts.size()+" charts",MARGIN+15,y+53);
        int areaX=MARGIN+LABEL_WIDTH+10,cardW=(SHELL-LABEL_WIDTH-20-CARD_GAP*(CARD_COLUMNS-1))/CARD_COLUMNS;
        for(int i=0;i<charts.size();i++){int col=i%CARD_COLUMNS,row=i/CARD_COLUMNS,x=areaX+col*(cardW+CARD_GAP),cy=y+10+row*(CARD_HEIGHT+CARD_GAP);drawCard(g,charts.get(i),records.get(charts.get(i).chartId()),jackets.get(charts.get(i).chartId()),x,cy,cardW,accent,level);}
    }
    private static void drawCard(Graphics2D g,ChartRating chart,ChartPlaydata record,BufferedImage jacket,int x,int y,int w,Color accent,int level){
        fill(g,Color.WHITE,x,y,w,CARD_HEIGHT);fill(g,accent,x,y,w,3);stroke(g,new Color(52,59,73),x,y,w,CARD_HEIGHT);
        if(jacket!=null)cover(g,jacket,x+1,y+3,w-2,62);else{fill(g,new Color(36,43,56),x+1,y+3,w-2,62);font(g,Font.BOLD,10);g.setColor(new Color(112,123,143));center(g,"popn.gg",x+1,y+3,w-2,62);}
        fill(g,new Color(250,251,253),x+1,y+65,w-2,23);fill(g,accent,x+7,y+70,34,13);font(g,Font.BOLD,8);g.setColor(Color.WHITE);center(g,difficulty(chart.difficulty())+" "+level,x+7,y+70,34,13);
        font(g,Font.BOLD,11);g.setColor(new Color(36,43,56));g.drawString(fit(g,chart.songName(),w-54),x+47,y+81);
        g.setColor(new Color(229,232,238));g.drawLine(x+1,y+88,x+w-1,y+88);font(g,Font.PLAIN,9);g.setColor(new Color(124,135,153));g.drawString(fit(g,chart.genreName(),w-100),x+7,y+106);
        if(record==null){font(g,Font.BOLD,10);g.setColor(new Color(154,163,178));g.drawString("NO PLAY",x+w-52,y+106);return;}
        drawMedal(g,record.medal().code(),x+w-82,y+94);font(g,Font.BOLD,11);g.setColor(new Color(20,26,38));String score=String.format("%,d",record.allTimeBest().score());g.drawString(score,x+w-7-g.getFontMetrics().stringWidth(score),y+106);
    }
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
    private static void drawFooter(Graphics2D g,int level,RatingController.Metric metric,int y){font(g,Font.PLAIN,10);g.setColor(new Color(89,100,119));g.drawString("popn.gg / personal tier map",MARGIN+3,y);String right="Lv"+level+" · "+metric+" · EXPERIMENTAL";g.drawString(right,WIDTH-MARGIN-g.getFontMetrics().stringWidth(right),y);}

    private BufferedImage jacket(String url){if(url==null||url.isBlank())return null;var cached=cache.get(url);if(cached!=null)return cached;try{var uri=URI.create(url);if(!"https".equals(uri.getScheme())||!"static.popn.gg".equalsIgnoreCase(uri.getHost()))return null;var response=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).header("Accept","image/*").GET().build(),HttpResponse.BodyHandlers.ofByteArray());if(response.statusCode()!=200||response.body().length>2_000_000)return null;var image=ImageIO.read(new ByteArrayInputStream(response.body()));if(image!=null)cache.put(url,image);return image;}catch(Exception ignored){return null;}}
    private static void cover(Graphics2D g,BufferedImage image,int x,int y,int w,int h){double scale=Math.max((double)w/image.getWidth(),(double)h/image.getHeight());int sw=(int)Math.round(w/scale),sh=(int)Math.round(h/scale),sx=(image.getWidth()-sw)/2,sy=(image.getHeight()-sh)/2;g.drawImage(image,x,y,x+w,y+h,sx,sy,sx+sw,sy+sh,null);}
    private static void paintBackground(Graphics2D g,int height){g.setPaint(new GradientPaint(0,0,new Color(26,32,48),0,Math.min(650,height),BG));g.fillRect(0,0,WIDTH,height);}
    private static void quality(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);}
    private static void font(Graphics2D g,int style,int size){g.setFont(new Font("Noto Sans CJK JP",style,size));}
    private static void fill(Graphics2D g,Color c,int x,int y,int w,int h){g.setColor(c);g.fillRect(x,y,w,h);}
    private static void stroke(Graphics2D g,Color c,int x,int y,int w,int h){g.setColor(c);g.drawRect(x,y,w-1,h-1);}
    private static void center(Graphics2D g,String text,int x,int y,int w,int h){var fm=g.getFontMetrics();g.drawString(text,x+(w-fm.stringWidth(text))/2,y+(h-fm.getHeight())/2+fm.getAscent());}
    private static String fit(Graphics2D g,String value,int width){if(value==null)return "";var fm=g.getFontMetrics();if(fm.stringWidth(value)<=width)return value;String ellipsis="…";int end=value.length();while(end>0&&fm.stringWidth(value.substring(0,end)+ellipsis)>width)end--;return value.substring(0,end)+ellipsis;}
    private static String fitWith(Font font,String value,int width){var image=new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setFont(font);String result=fit(g,value,width);g.dispose();return result;}
}
