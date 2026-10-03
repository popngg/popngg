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
    static final int WIDTH = 1160;
    private static final int MARGIN = 28, SHELL = WIDTH - MARGIN * 2, LABEL_WIDTH = 90;
    private static final int CARD_GAP = 4, CARD_COLUMNS = 5, CARD_HEIGHT = 111, MAX_BAND_SIZE = 20;
    private static final int FEATURE_BADGE_WIDTH = 30, TOOLBAR_HEIGHT = 52;
    private static final Color BG = Color.WHITE, TEXT = new Color(26, 28, 32);
    private static final Color SUBTLE = new Color(85, 93, 109), MUTED = new Color(134, 139, 148);
    private static final Color BORDER = new Color(220, 222, 227);
    private static final Color[] ACCENTS = {new Color(239, 68, 68), new Color(249, 115, 22),
            new Color(217, 70, 239), new Color(139, 92, 246), new Color(59, 130, 246),
            new Color(6, 182, 212), new Color(34, 197, 94), new Color(234, 179, 8)};
    private static final String[] MEDAL_FILES = {"none", "gold-star", "silver-star", "silver-diamond",
            "silver-circle", "bronze-star", "bronze-diamond", "bronze-circle", "black-star",
            "black-diamond", "black-circle", "easy", "long-off", "none"};
    private static final String[] RANK_FILES = {"none", "s-plus", "s", "aaa", "aa-plus", "aa",
            "a-plus", "a", "b-plus", "b", "c", "d", "e", "none"};
    private static final Map<Integer, BufferedImage> MEDAL_ICONS = loadIcons("medals", MEDAL_FILES);
    private static final Map<Integer, BufferedImage> RANK_ICONS = loadIcons("ranks", RANK_FILES);
    private static final String FONT_FAMILY = fontFamily();

    private final HttpClient http;
    private final ExecutorService jacketExecutor = Executors.newFixedThreadPool(8);
    private final Map<String, BufferedImage> cache = Collections.synchronizedMap(new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) { return size() > 256; }
    });
    private final Map<String, byte[]> rendered = Collections.synchronizedMap(new LinkedHashMap<>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) { return size() > 32; }
    });

    public TierListImageRenderer() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    TierListImageRenderer(HttpClient http) { this.http = http; }

    public byte[] render(RatingSnapshot snapshot, UserPlaydata user, UserProfileResult profile,
                         int level, RatingController.Metric metric) throws IOException {
        String renderKey = snapshot.snapshotId() + ":" + user.poptomoId() + ":" + profile.updatedAt()
                + ":" + user.playdata().hashCode() + ":" + level + ":" + metric;
        var ready = rendered.get(renderKey);
        if (ready != null) return ready;
        var records = new HashMap<Long, ChartPlaydata>();
        user.playdata().forEach(record -> records.put(record.chartId(), record));
        var eligible = snapshot.charts().stream().filter(chart -> chart.level() == level)
                .filter(chart -> "ELIGIBLE".equals(status(chart, metric))).sorted(metric.comparator()).toList();
        var held = snapshot.charts().stream().filter(chart -> chart.level() == level)
                .filter(chart -> !"ELIGIBLE".equals(status(chart, metric))).toList();
        var bands = bands(eligible, metric);
        var all = new ArrayList<ChartRating>(eligible); all.addAll(held);
        var jackets = new ConcurrentHashMap<Long, BufferedImage>();
        var loads = all.stream().map(chart -> CompletableFuture.runAsync(() -> {
            var loaded = jacket(chart.jacketUrl());
            if (loaded != null) jackets.put(chart.chartId(), loaded);
        }, jacketExecutor)).toList();
        loads.forEach(CompletableFuture::join);
        var avatar = remoteImage(profile.profileImageUrl());

        int height = MARGIN + headerHeight() + TOOLBAR_HEIGHT
                + bands.stream().mapToInt(band -> bandHeight(band.size())).sum()
                + (held.isEmpty() ? 0 : bandHeight(held.size()) + 8) + 22;
        var image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); quality(g); paintBackground(g, height);
        int y = MARGIN;
        drawHeader(g, user, profile, avatar, level, metric, all, records, y); y += headerHeight();
        drawToolbar(g, y); y += TOOLBAR_HEIGHT;
        int index = 0;
        for (var band : bands) {
            int h = bandHeight(band.size());
            drawBand(g, bandLabel(index), band, y, ACCENTS[index % ACCENTS.length], records, jackets, level, metric);
            y += h; index++;
        }
        if (!held.isEmpty()) {
            y += 8;
            drawBand(g, "판정보류", held, y, new Color(141, 113, 142), records, jackets, level, metric);
        }
        g.dispose();
        try (var out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out)) throw new IOException("PNG_WRITER_UNAVAILABLE");
            byte[] png = out.toByteArray(); rendered.put(renderKey, png); return png;
        }
    }

    @PreDestroy void shutdown() { jacketExecutor.shutdown(); }

    static List<List<ChartRating>> bands(List<ChartRating> charts, RatingController.Metric metric) {
        if (charts.isEmpty()) return List.of();
        if (charts.size() == 1) return List.of(charts);
        var gaps = new ArrayList<Double>();
        for (int i = 0; i < charts.size() - 1; i++)
            gaps.add(Math.abs(value(charts.get(i), metric) - value(charts.get(i + 1), metric)));
        var sorted = new ArrayList<>(gaps); Collections.sort(sorted);
        double median = quantile(sorted, .5), q1 = quantile(sorted, .25), q3 = quantile(sorted, .75);
        var deviations = sorted.stream().map(value -> Math.abs(value - median)).sorted().toList();
        double mad = quantile(deviations, .5);
        double threshold = Math.max(median + 4 * Math.max(mad, 1e-9),
                q3 + 1.5 * Math.max(q3 - q1, 1e-9));
        var breaks = new TreeSet<Integer>();
        var significant = new ArrayList<Integer>();
        for (int i = 0; i < gaps.size(); i++) if (gaps.get(i) > threshold) significant.add(i + 1);
        significant.sort(Comparator.comparingDouble((Integer boundary) -> gaps.get(boundary - 1)).reversed());
        for (int boundary : significant) {
            if (boundary < 3 || charts.size() - boundary < 3) continue;
            if (breaks.stream().allMatch(existing -> Math.abs(existing - boundary) >= 3)) breaks.add(boundary);
        }
        while (largestSegment(breaks, charts.size()).size() > MAX_BAND_SIZE) {
            Segment segment = largestSegment(breaks, charts.size());
            int minimum = segment.start() + 3, maximum = segment.end() - 3;
            int target = (segment.start() + segment.end()) / 2, best = minimum;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int boundary = minimum; boundary <= maximum; boundary++) {
                double score = gaps.get(boundary - 1)
                        - Math.abs(boundary - target) * Math.max(median, 1e-9) * .15;
                if (score > bestScore) { bestScore = score; best = boundary; }
            }
            breaks.add(best);
        }
        var result = new ArrayList<List<ChartRating>>(); int start = 0;
        for (int boundary : breaks) { result.add(charts.subList(start, boundary)); start = boundary; }
        result.add(charts.subList(start, charts.size())); return result;
    }

    private static Segment largestSegment(SortedSet<Integer> breaks, int size) {
        int start = 0; Segment largest = new Segment(0, 0);
        for (int end : breaks) {
            if (end - start > largest.size()) largest = new Segment(start, end);
            start = end;
        }
        if (size - start > largest.size()) largest = new Segment(start, size);
        return largest;
    }

    private static double quantile(List<Double> values, double q) {
        double position = (values.size() - 1) * q; int low = (int) position, high = (int) Math.ceil(position);
        return values.get(low) + (values.get(high) - values.get(low)) * (position - low);
    }
    private static double value(ChartRating chart, RatingController.Metric metric) {
        return metric == RatingController.Metric.CPI ? chart.cpi() : chart.spi();
    }
    private static String status(ChartRating chart, RatingController.Metric metric) {
        return metric == RatingController.Metric.CPI ? chart.cpiEligibilityStatus() : chart.spiEligibilityStatus();
    }
    private static int headerHeight() { return 252; }
    private static int bandHeight(int songs) {
        return Math.max(1, (songs + CARD_COLUMNS - 1) / CARD_COLUMNS) * (CARD_HEIGHT + CARD_GAP) + 14;
    }

    private void drawHeader(Graphics2D g, UserPlaydata user, UserProfileResult profile, BufferedImage avatar,
                            int level, RatingController.Metric metric, List<ChartRating> charts,
                            Map<Long, ChartPlaydata> records, int y) {
        drawAvatar(g, avatar, MARGIN, y + 4, 112);
        int identityX = MARGIN + 142;
        font(g, Font.BOLD, 36); g.setColor(TEXT); g.drawString(fit(g, user.userName(), 500), identityX, y + 43);
        font(g, Font.PLAIN, 13); String poptomo = "#  " + user.poptomoId();
        int idWidth = Math.max(154, g.getFontMetrics().stringWidth(poptomo) + 24);
        roundedFill(g, new Color(243, 244, 245), identityX, y + 57, idWidth, 32, 7);
        g.setColor(SUBTLE); center(g, poptomo, identityX, y + 57, idWidth, 32);
        font(g, Font.BOLD, 13); g.setColor(new Color(207, 49, 49));
        g.drawString(fit(g, "♥  " + valueOrDash(profile.characterName()), 570), identityX, y + 119);
        g.setColor(new Color(229, 231, 235)); g.fillRect(identityX, y + 139, 3, 27);
        font(g, Font.PLAIN, 13); g.setColor(SUBTLE);
        g.drawString(fit(g, valueOrDash(profile.comment()), 570), identityX + 12, y + 158);
        drawProfileStats(g, profile, MARGIN + SHELL - 310, y + 5, 310);

        String title = "Lv" + level + " " + (metric == RatingController.Metric.CPI
                ? "클리어 난이도 서열표" : "스코어 난이도 서열표");
        font(g, Font.BOLD, 24); g.setColor(TEXT); g.drawString(title, MARGIN, y + 211);
        font(g, Font.PLAIN, 11); g.setColor(MUTED);
        g.drawString("위쪽일수록 어려운 채보입니다 · " + metric.name() + " 기준", MARGIN, y + 234);
        var summary = summary(charts, records);
        drawProgressStat(g, "PLAYED", summary.played(), summary.total(), MARGIN + SHELL - 300, y + 195);
        drawProgressStat(g, "CLEAR", summary.cleared(), summary.played(), MARGIN + SHELL - 145, y + 195);
        if (summary.played() > 0) {
            font(g, Font.PLAIN, 10); g.setColor(MUTED);
            g.drawString(String.format("%.1f%%", 100d * summary.cleared() / summary.played()),
                    MARGIN + SHELL - 145, y + 239);
        }
    }

    private static void drawProfileStats(Graphics2D g, UserProfileResult profile, int x, int y, int width) {
        font(g, Font.PLAIN, 11); g.setColor(MUTED); g.drawString("팝픈클래스", x, y + 8);
        font(g, Font.BOLD, 28); g.setColor(TEXT);
        String current = String.format("%.2f", profile.displayPopclass() / 1000d);
        g.drawString(current, x, y + 43); int currentWidth = g.getFontMetrics().stringWidth(current);
        font(g, Font.PLAIN, 12); g.setColor(SUBTLE);
        g.drawString("/ 구" + String.format("%.2f", profile.legacyPopclass() / 100d), x + currentWidth + 9, y + 42);
        g.setColor(BORDER); g.drawLine(x, y + 61, x + width, y + 61);
        var summaries = new HashMap<String, UserProfileResult.MedalSummary>();
        profile.medalSummaries().forEach(summary -> summaries.put(summary.kind(), summary));
        profileStat(g, "클리어", level(summaries, "clear"), new Color(255, 77, 128), x, y + 86, width);
        profileStat(g, "풀콤보", level(summaries, "full-combo"), new Color(50, 203, 204), x, y + 116, width);
        profileStat(g, "퍼펙트", level(summaries, "perfect"), new Color(255, 191, 0), x, y + 146, width);
    }

    private static void profileStat(Graphics2D g, String label, int level, Color color, int x, int y, int width) {
        g.setColor(color); g.fillOval(x, y - 8, 7, 7); font(g, Font.BOLD, 12); g.drawString(label, x + 15, y);
        font(g, Font.PLAIN, 15); g.setColor(TEXT); String value = level == 0 ? "—" : Integer.toString(level);
        g.drawString(value, x + width - 31 - g.getFontMetrics().stringWidth(value), y);
        font(g, Font.PLAIN, 10); g.setColor(MUTED); g.drawString("Lv", x + width - 25, y);
    }

    private static void drawProgressStat(Graphics2D g, String label, long current, long total, int x, int y) {
        font(g, Font.BOLD, 9); g.setColor(MUTED); g.drawString(label, x, y);
        font(g, Font.BOLD, 20); g.setColor(TEXT); String primary = Long.toString(current);
        g.drawString(primary, x, y + 26); int primaryWidth = g.getFontMetrics().stringWidth(primary);
        font(g, Font.PLAIN, 11); g.setColor(MUTED); g.drawString("/ " + total, x + primaryWidth + 6, y + 25);
    }

    private static int level(Map<String, UserProfileResult.MedalSummary> summaries, String kind) {
        var summary = summaries.get(kind); return summary == null ? 0 : summary.maxLevel();
    }
    private static String valueOrDash(String value) { return value == null || value.isBlank() ? "—" : value; }

    private static void drawToolbar(Graphics2D g, int y) {
        font(g, Font.BOLD, 10); g.setColor(SUBTLE); g.drawString("범례", MARGIN, y + 18);
        int x = MARGIN + 42;
        x = drawLegendItem(g, "個", "개인차", new Color(75, 123, 229), x, y + 4);
        x = drawLegendItem(g, "辛G", "짠게", new Color(234, 96, 104), x, y + 4);
        x = drawLegendItem(g, "辛判", "짠판", new Color(247, 196, 81), x, y + 4);
        x = drawLegendItem(g, "EX", "EXTRA", new Color(139, 92, 246), x, y + 4);
        drawLegendItem(g, "超EX", "초EXTRA", new Color(91, 33, 182), x, y + 4);
    }

    private void drawBand(Graphics2D g, String label, List<ChartRating> charts, int y, Color accent,
                          Map<Long, ChartPlaydata> records, Map<Long, BufferedImage> jackets,
                          int level, RatingController.Metric metric) {
        font(g, Font.BOLD, 14); g.setColor(accent); g.drawString(label, MARGIN, y + 24);
        font(g, Font.PLAIN, 9); g.setColor(MUTED); g.drawString(charts.size() + "곡", MARGIN, y + 41);
        if (!"판정보류".equals(label)) g.drawString(metricRange(charts, metric), MARGIN, y + 57);
        int areaX = MARGIN + LABEL_WIDTH + 10;
        int cardWidth = (SHELL - LABEL_WIDTH - 10 - CARD_GAP * (CARD_COLUMNS - 1)) / CARD_COLUMNS;
        for (int i = 0; i < charts.size(); i++) {
            int x = areaX + i % CARD_COLUMNS * (cardWidth + CARD_GAP);
            int cardY = y + i / CARD_COLUMNS * (CARD_HEIGHT + CARD_GAP);
            var chart = charts.get(i);
            drawCard(g, chart, records.get(chart.chartId()), jackets.get(chart.chartId()), x, cardY, cardWidth, level, metric);
        }
    }

    private static void drawCard(Graphics2D g, ChartRating chart, ChartPlaydata record,
                                 BufferedImage jacket, int x, int y, int width, int level,
                                 RatingController.Metric metric) {
        int medalCode = isPlayed(record) && record.medal() != null ? record.medal().code() : 13;
        Color clearColor = clearTypeColor(medalCode);
        fill(g, Color.WHITE, x, y, width, CARD_HEIGHT);
        int bannerHeight = 47;
        if (jacket != null) drawBanner(g, jacket, x + 1, y + 1, width - 2, bannerHeight - 1);
        else {
            fill(g, new Color(238, 239, 241), x + 1, y + 1, width - 2, bannerHeight - 1);
            font(g, Font.BOLD, 10); g.setColor(new Color(176, 179, 186));
            center(g, "popn.gg", x + 1, y + 1, width - 2, bannerHeight - 1);
        }
        drawFeatureBadges(g, chart, metric, x + 4, y + 1);
        int titleY = y + bannerHeight, difficultyWidth = 40;
        fill(g, mix(Color.WHITE, clearColor, .15), x + 1, titleY, width - 2, 24);
        fill(g, difficultyColor(chart.difficulty()), x + 1, titleY, difficultyWidth, 24);
        font(g, Font.BOLD, 9); g.setColor(Color.WHITE);
        center(g, difficulty(chart.difficulty()) + " " + level, x + 1, titleY, difficultyWidth, 24);
        font(g, Font.PLAIN, 10); g.setColor(TEXT);
        String title = fit(g, chartTitle(chart), width - difficultyWidth - 10);
        g.drawString(title, x + width - 6 - g.getFontMetrics().stringWidth(title), titleY + 16);
        int resultY = titleY + 24;
        font(g, Font.PLAIN, 10); g.setColor(MUTED); g.drawString(metricValue(chart, metric), x + 7, resultY + 25);
        if (!isPlayed(record)) {
            font(g, Font.BOLD, 9); g.setColor(MUTED); String noPlay = "NO PLAY";
            g.drawString(noPlay, x + width - 7 - g.getFontMetrics().stringWidth(noPlay), resultY + 25);
        } else {
            int score = record.allTimeBest() == null ? 0 : record.allTimeBest().score();
            int rankCode = record.allTimeBest() == null || record.allTimeBest().rankCode() == null
                    ? 13 : record.allTimeBest().rankCode();
            font(g, Font.BOLD, 10); String scoreText = String.format("%,d", score);
            int scoreX = x + width - 7 - g.getFontMetrics().stringWidth(scoreText);
            int rankX = scoreX - 25, medalX = rankX - 24;
            drawIcon(g, MEDAL_ICONS.getOrDefault(medalCode, MEDAL_ICONS.get(13)), medalX, resultY + 9, 20, 20);
            drawIcon(g, RANK_ICONS.getOrDefault(rankCode, RANK_ICONS.get(13)), rankX, resultY + 9, 20, 20);
            g.setColor(TEXT); g.drawString(scoreText, scoreX, resultY + 25);
        }
        stroke(g, clearColor, x, y, width, CARD_HEIGHT);
    }

    private static String metricValue(ChartRating chart, RatingController.Metric metric) {
        Double rating = metric == RatingController.Metric.CPI ? chart.cpi() : chart.spi();
        if (rating == null) return "—";
        return metric == RatingController.Metric.CPI ? String.format("%+.2f", rating) : String.format("%,.0f", rating);
    }
    private static String metricRange(List<ChartRating> charts, RatingController.Metric metric) {
        return charts.isEmpty() ? "" : metricValue(charts.getFirst(), metric) + " – " + metricValue(charts.getLast(), metric);
    }

    private static void drawFeatureBadges(Graphics2D g, ChartRating chart, RatingController.Metric metric, int x, int y) {
        var features = new ArrayList<Feature>();
        String individuality = metric == RatingController.Metric.CPI
                ? chart.cpiIndividualityStatus() : chart.spiIndividualityStatus();
        if ("CANDIDATE".equals(individuality) || "CONFIRMED".equals(individuality))
            features.add(new Feature("個", new Color(75,123,229), new Color(50,103,214), new Color(40,84,184), new Color(120,162,255), Color.WHITE));
        if (chart.strictGauge())
            features.add(new Feature("辛G", new Color(234,96,104), new Color(217,74,82), new Color(185,54,64), new Color(255,133,139), Color.WHITE));
        if (chart.strictJudgement())
            features.add(new Feature("辛判", new Color(247,196,81), new Color(239,174,50), new Color(213,138,24), new Color(255,217,120), new Color(33,23,6)));
        if ("EXTRA".equals(chart.extraType()))
            features.add(new Feature("EX", new Color(167,139,250), new Color(139,92,246), new Color(109,40,217), new Color(196,181,253), Color.WHITE));
        if ("SUPER_EXTRA".equals(chart.extraType()))
            features.add(new Feature("超EX", new Color(124,58,237), new Color(91,33,182), new Color(76,29,149), new Color(167,139,250), Color.WHITE));
        int offset = 0;
        for (var feature : features) { drawFeatureBadge(g, feature, x + offset, y); offset += FEATURE_BADGE_WIDTH + 2; }
    }

    private static void drawFeatureBadge(Graphics2D g, Feature feature, int x, int y) {
        int width = FEATURE_BADGE_WIDTH, height = 27; var shape = new Path2D.Double();
        shape.moveTo(x,y); shape.lineTo(x+width,y); shape.lineTo(x+width,y+21);
        shape.lineTo(x+width/2.0,y+height); shape.lineTo(x,y+21); shape.closePath();
        var paint = g.getPaint();
        g.setPaint(new LinearGradientPaint(x,y,x,y+height,new float[]{0,.48f,1},
                new Color[]{feature.highlight(),feature.main(),feature.dark()}));
        g.fill(shape); g.setPaint(paint); g.setColor(feature.border()); g.draw(shape);
        font(g,Font.BOLD,9); g.setColor(feature.text()); center(g,feature.mark(),x,y-1,width,22);
    }

    private static int drawLegendItem(Graphics2D g, String mark, String label, Color color, int x, int y) {
        fill(g,color,x,y,FEATURE_BADGE_WIDTH,16); font(g,Font.BOLD,8); g.setColor(Color.WHITE);
        center(g,mark,x,y,FEATURE_BADGE_WIDTH,16); font(g,Font.BOLD,9); g.setColor(MUTED);
        g.drawString(label,x+FEATURE_BADGE_WIDTH+5,y+13);
        return x+FEATURE_BADGE_WIDTH+5+g.getFontMetrics().stringWidth(label)+16;
    }

    static String chartTitle(ChartRating chart) { return (chart.upper() ? "Ⓤ " : "") + chart.songName(); }
    static boolean isPlayed(ChartPlaydata record) {
        return record != null && ((record.medal() != null && record.medal().code() >= 1 && record.medal().code() <= 12)
                || (record.allTimeBest() != null && record.allTimeBest().score() > 0));
    }
    static LevelSummary summary(List<ChartRating> charts, Map<Long, ChartPlaydata> records) {
        long played = charts.stream().filter(chart -> isPlayed(records.get(chart.chartId()))).count();
        long cleared = charts.stream().filter(chart -> {
            var record = records.get(chart.chartId());
            return record != null && record.medal() != null && isCleared(record.medal().code());
        }).count();
        return new LevelSummary(charts.size(), played, cleared);
    }
    record LevelSummary(int total, long played, long cleared) {}

    static String medalIconResource(int code) {
        int normalized = code >= 1 && code <= 13 ? code : 13;
        return "/medals/" + MEDAL_FILES[normalized] + ".png";
    }
    static String rankIconResource(int code) {
        int normalized = code >= 1 && code <= 13 ? code : 13;
        return "/ranks/" + RANK_FILES[normalized] + ".png";
    }
    private static Map<Integer, BufferedImage> loadIcons(String directory, String[] files) {
        var result = new HashMap<Integer, BufferedImage>();
        for (int code=1; code<=13; code++) {
            String resource = "/" + directory + "/" + files[code] + ".png";
            try (var in = TierListImageRenderer.class.getResourceAsStream(resource)) {
                if (in == null) throw new IllegalStateException("Missing icon: " + resource);
                var image=ImageIO.read(in); if(image==null) throw new IllegalStateException("Invalid icon: "+resource);
                result.put(code,image);
            } catch(IOException exception) { throw new UncheckedIOException(exception); }
        }
        return Map.copyOf(result);
    }

    static boolean isCleared(int code) { return code>=1 && code<=7 || code==11 || code==12; }
    private static Color clearTypeColor(int code) {
        if(code==1)return new Color(255,191,0);
        if(code>=2&&code<=4)return new Color(50,203,204);
        if(code>=5&&code<=7)return new Color(255,77,128);
        if(code==11||code==12)return new Color(132,224,172);
        return new Color(138,168,214);
    }
    private static Color difficultyColor(int code) {
        return switch(code){case 1->new Color(69,189,232);case 2->new Color(85,185,120);
            case 3->new Color(217,157,32);case 4->new Color(227,91,121);default->MUTED;};
    }
    private static String difficulty(int code) {
        return switch(code){case 1->"L";case 2->"N";case 3->"H";case 4->"EX";default->"?";};
    }

    private BufferedImage jacket(String url){return remoteImage(url);}
    private BufferedImage remoteImage(String url){
        if(url==null||url.isBlank())return null;var cached=cache.get(url);if(cached!=null)return cached;
        try{var uri=URI.create(url);if(!"https".equals(uri.getScheme())||!"static.popn.gg".equalsIgnoreCase(uri.getHost()))return null;
            var response=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).header("Accept","image/*").GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode()!=200||response.body().length>2_000_000)return null;
            var image=ImageIO.read(new ByteArrayInputStream(response.body()));if(image!=null)cache.put(url,image);return image;
        }catch(Exception ignored){return null;}}

    private static void drawAvatar(Graphics2D g,BufferedImage avatar,int x,int y,int size){
        var oldClip=g.getClip();var shape=new RoundRectangle2D.Double(x,y,size,size,36,36);g.setClip(shape);
        if(avatar==null){fill(g,new Color(243,244,245),x,y,size,size);font(g,Font.BOLD,22);g.setColor(MUTED);center(g,"popn",x,y,size,size);}else cover(g,avatar,x,y,size,size);
        g.setClip(oldClip);g.setColor(new Color(229,231,235));g.draw(shape);
    }
    private static void drawBanner(Graphics2D g,BufferedImage image,int x,int y,int width,int height){
        Shape oldClip=g.getClip();Composite oldComposite=g.getComposite();g.setClip(x,y,width,height);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.28f));cover(g,image,x,y,width,height);
        g.setComposite(oldComposite);contain(g,image,x,y,width,height);g.setClip(oldClip);
    }
    private static void cover(Graphics2D g,BufferedImage image,int x,int y,int width,int height){
        double scale=Math.max((double)width/image.getWidth(),(double)height/image.getHeight());
        int sw=(int)Math.round(width/scale),sh=(int)Math.round(height/scale),sx=(image.getWidth()-sw)/2,sy=(image.getHeight()-sh)/2;
        g.drawImage(image,x,y,x+width,y+height,sx,sy,sx+sw,sy+sh,null);
    }
    private static void contain(Graphics2D g,BufferedImage image,int x,int y,int width,int height){
        double scale=Math.min((double)width/image.getWidth(),(double)height/image.getHeight());
        int tw=(int)Math.round(image.getWidth()*scale),th=(int)Math.round(image.getHeight()*scale);
        g.drawImage(image,x+(width-tw)/2,y+(height-th)/2,tw,th,null);
    }
    private static void drawIcon(Graphics2D g,BufferedImage image,int x,int y,int width,int height){if(image!=null)contain(g,image,x,y,width,height);}
    private static void paintBackground(Graphics2D g,int height){g.setColor(BG);g.fillRect(0,0,WIDTH,height);g.setPaint(new GradientPaint(0,0,new Color(255,243,243),0,250,Color.WHITE));g.fillRect(0,0,WIDTH,250);}
    private static void quality(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);}
    private static String fontFamily(){var available=Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());for(String candidate:List.of("Noto Sans CJK KR","Noto Sans CJK JP","Noto Sans KR","SansSerif"))if(available.contains(candidate))return candidate;return "SansSerif";}
    private static void font(Graphics2D g,int style,int size){g.setFont(new Font(FONT_FAMILY,style,size));}
    private static void fill(Graphics2D g,Color color,int x,int y,int width,int height){g.setColor(color);g.fillRect(x,y,width,height);}
    private static void roundedFill(Graphics2D g,Color color,int x,int y,int width,int height,int radius){g.setColor(color);g.fillRoundRect(x,y,width,height,radius,radius);}
    private static void stroke(Graphics2D g,Color color,int x,int y,int width,int height){g.setColor(color);g.drawRect(x,y,width-1,height-1);}
    private static void center(Graphics2D g,String text,int x,int y,int width,int height){var metrics=g.getFontMetrics();g.drawString(text,x+(width-metrics.stringWidth(text))/2,y+(height-metrics.getHeight())/2+metrics.getAscent());}
    private static String fit(Graphics2D g,String value,int width){if(value==null)return "";var metrics=g.getFontMetrics();if(metrics.stringWidth(value)<=width)return value;String ellipsis="…";int end=value.length();while(end>0&&metrics.stringWidth(value.substring(0,end)+ellipsis)>width)end--;return value.substring(0,end)+ellipsis;}
    private static Color mix(Color base,Color tint,double amount){return new Color((int)Math.round(base.getRed()*(1-amount)+tint.getRed()*amount),(int)Math.round(base.getGreen()*(1-amount)+tint.getGreen()*amount),(int)Math.round(base.getBlue()*(1-amount)+tint.getBlue()*amount));}
    private static String bandLabel(int index){return "난이도군 "+(char)('A'+index);}
    private record Feature(String mark,Color highlight,Color main,Color dark,Color border,Color text){}
    private record Segment(int start,int end){int size(){return end-start;}}
}
