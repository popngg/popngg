package gg.popn.http.analysis;

import gg.popn.application.analysis.AchievementConstants;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

@Component
public class AchievementConstantImageRenderer {
    static final int WIDTH = 1160;
    private static final int MARGIN = 28, COLUMNS = 5, GAP = 6, CARD_HEIGHT = 154;
    private static final int CONTENT = WIDTH - MARGIN * 2;
    private static final int CARD_WIDTH = (CONTENT - GAP * (COLUMNS - 1)) / COLUMNS;
    private static final Color TEXT = new Color(26, 28, 32), MUTED = new Color(118, 124, 135);
    private static final Color BORDER = new Color(220, 222, 227), HOLD = new Color(141, 113, 142);
    private static final String FONT = fontFamily();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'KST'")
            .withZone(ZoneId.of("Asia/Seoul"));
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final Map<String, BufferedImage> cache = Collections.synchronizedMap(new LinkedHashMap<>(256,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,BufferedImage> eldest){return size()>256;}
    });

    public byte[] render(AchievementConstants.Snapshot snapshot) throws IOException {
        String reference = snapshot.axis() == AchievementConstants.Axis.MEDAL ? "CLEAR" : "AAA";
        var charts = new ArrayList<>(snapshot.charts());
        charts.sort(Comparator
                .comparing((AchievementConstants.Chart c) -> referenceValue(c, reference),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AchievementConstants.Chart::songName));
        var jackets = new ConcurrentHashMap<Long, BufferedImage>();
        var tasks = charts.stream().map(chart -> CompletableFuture.runAsync(() -> {
            BufferedImage jacket = remote(chart.jacketUrl()); if (jacket != null) jackets.put(chart.chartId(), jacket);
        }, executor)).toList();
        tasks.forEach(CompletableFuture::join);
        int rows = Math.max(1, (charts.size() + COLUMNS - 1) / COLUMNS);
        int height = 166 + rows * (CARD_HEIGHT + GAP) + 30;
        var image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); quality(g);
        g.setColor(Color.WHITE); g.fillRect(0,0,WIDTH,height);
        g.setPaint(new GradientPaint(0,0,new Color(255,243,243),0,170,Color.WHITE)); g.fillRect(0,0,WIDTH,170);
        drawHeader(g, snapshot, charts.size());
        for (int i=0;i<charts.size();i++) {
            int x=MARGIN+(i%COLUMNS)*(CARD_WIDTH+GAP), y=166+(i/COLUMNS)*(CARD_HEIGHT+GAP);
            drawCard(g, charts.get(i), jackets.get(charts.get(i).chartId()), snapshot.axis(), x, y);
        }
        g.dispose();
        try(var output=new ByteArrayOutputStream()) {
            if(!ImageIO.write(image,"png",output)) throw new IOException("PNG_WRITER_UNAVAILABLE");
            return output.toByteArray();
        }
    }

    private static void drawHeader(Graphics2D g, AchievementConstants.Snapshot snapshot, int chartCount) {
        font(g,Font.BOLD,30); g.setColor(TEXT);
        String axis=snapshot.axis()==AchievementConstants.Axis.MEDAL?"메달":"스코어 랭크";
        g.drawString("Lv"+snapshot.level()+" "+axis+" 달성 난이도 상수표",MARGIN,48);
        font(g,Font.PLAIN,13);g.setColor(MUTED);
        g.drawString("관리자 검토용 · 각 곡의 목표별 상대 난이도를 레벨 상수로 환산",MARGIN,76);
        font(g,Font.BOLD,12);g.setColor(TEXT);g.drawString("곡 "+chartCount+"개",MARGIN,111);
        font(g,Font.PLAIN,11);g.setColor(MUTED);
        g.drawString("모델 "+snapshot.modelVersion()+" · "+TIME.format(snapshot.generatedAt()),MARGIN,135);
        String note="값이 높을수록 해당 목표 달성이 어렵습니다 · — 는 판정보류";
        g.drawString(note,WIDTH-MARGIN-g.getFontMetrics().stringWidth(note),135);
    }

    private static void drawCard(Graphics2D g, AchievementConstants.Chart chart, BufferedImage jacket,
                                 AchievementConstants.Axis axis, int x, int y) {
        g.setColor(Color.WHITE);g.fillRoundRect(x,y,CARD_WIDTH,CARD_HEIGHT,8,8);
        Shape old=g.getClip();g.setClip(new RoundRectangle2D.Double(x,y,CARD_WIDTH,58,8,8));
        if(jacket==null){g.setColor(new Color(241,242,244));g.fillRect(x,y,CARD_WIDTH,58);}
        else contain(g,jacket,x,y,CARD_WIDTH,58);
        g.setClip(old);
        font(g,Font.BOLD,11);g.setColor(TEXT);
        g.drawString(fit(g,(chart.upper()?"Ⓤ ":"")+chart.songName(),CARD_WIDTH-14),x+7,y+75);
        drawFlags(g,chart,x+7,y+82);
        List<String> targets=axis==AchievementConstants.Axis.MEDAL
                ?List.of("CLEAR","BRONZE_DIAMOND","BRONZE_STAR","FULL_COMBO","PERFECT")
                :List.of("AA","AA_PLUS","AAA","S","S_PLUS");
        var values=new HashMap<String,AchievementConstants.Constant>();
        chart.constants().forEach(value->values.put(value.target(),value));
        int cell=(CARD_WIDTH-14)/3;
        for(int i=0;i<targets.size();i++){
            int row=i/3,col=i%3,cx=x+7+col*cell,cy=y+108+row*25;
            String target=targets.get(i);AchievementConstants.Constant value=values.get(target);
            font(g,Font.BOLD,8);g.setColor(MUTED);g.drawString(label(target),cx,cy);
            font(g,Font.BOLD,11);g.setColor(value!=null&&value.value()!=null?TEXT:HOLD);
            String formatted=value==null||value.value()==null?"—":String.format(Locale.ROOT,"%.2f",value.value());
            g.drawString(formatted,cx,cy+13);
        }
        Color edge=chart.strictGauge()?new Color(234,96,104):chart.strictJudgement()?new Color(239,174,50):BORDER;
        g.setColor(edge);g.drawRoundRect(x,y,CARD_WIDTH-1,CARD_HEIGHT-1,8,8);
    }

    private static void drawFlags(Graphics2D g, AchievementConstants.Chart chart,int x,int y){
        var flags=new ArrayList<String>();
        if(chart.strictGauge())flags.add("짠게");if(chart.strictJudgement())flags.add("짠판");
        if("EXTRA".equals(chart.extraType()))flags.add("EX");if("SUPER_EXTRA".equals(chart.extraType()))flags.add("超EX");
        font(g,Font.BOLD,8);for(String flag:flags){int width=Math.max(24,g.getFontMetrics().stringWidth(flag)+8);
            g.setColor(flag.contains("EX")?new Color(139,92,246):new Color(239,174,50));g.fillRoundRect(x,y,width,14,5,5);
            g.setColor(Color.WHITE);center(g,flag,x,y,width,14);x+=width+3;}
    }

    private BufferedImage remote(String url){
        if(url==null||url.isBlank())return null;var ready=cache.get(url);if(ready!=null)return ready;
        try{URI uri=URI.create(url);if(!"https".equals(uri.getScheme())||!"static.popn.gg".equalsIgnoreCase(uri.getHost()))return null;
            var response=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode()!=200||response.body().length>2_000_000)return null;
            var image=ImageIO.read(new ByteArrayInputStream(response.body()));if(image!=null)cache.put(url,image);return image;
        }catch(Exception ignored){return null;}
    }
    private static Double referenceValue(AchievementConstants.Chart chart,String target){return chart.constants().stream()
            .filter(c->target.equals(c.target())).map(AchievementConstants.Constant::value)
            .filter(Objects::nonNull).findFirst().orElse(null);}
    private static String label(String target){return switch(target){case"BRONZE_DIAMOND"->"동다";case"BRONZE_STAR"->"동별";
        case"FULL_COMBO"->"FC";case"PERFECT"->"PERFECT";case"AA_PLUS"->"AA+";case"S_PLUS"->"S+";default->target;};}
    private static void contain(Graphics2D g,BufferedImage image,int x,int y,int w,int h){double s=Math.min((double)w/image.getWidth(),(double)h/image.getHeight());
        int tw=(int)Math.round(image.getWidth()*s),th=(int)Math.round(image.getHeight()*s);g.drawImage(image,x+(w-tw)/2,y+(h-th)/2,tw,th,null);}
    private static String fit(Graphics2D g,String value,int width){if(g.getFontMetrics().stringWidth(value)<=width)return value;int end=value.length();
        while(end>0&&g.getFontMetrics().stringWidth(value.substring(0,end)+"…")>width)end--;return value.substring(0,end)+"…";}
    private static void center(Graphics2D g,String value,int x,int y,int w,int h){var m=g.getFontMetrics();g.drawString(value,x+(w-m.stringWidth(value))/2,y+(h-m.getHeight())/2+m.getAscent());}
    private static void font(Graphics2D g,int style,int size){g.setFont(new Font(FONT,style,size));}
    private static void quality(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);}
    private static String fontFamily(){var names=Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for(String candidate:List.of("Noto Sans CJK KR","Noto Sans CJK JP","Noto Sans KR","SansSerif"))if(names.contains(candidate))return candidate;return"SansSerif";}
    @PreDestroy void close(){executor.shutdownNow();}
}
