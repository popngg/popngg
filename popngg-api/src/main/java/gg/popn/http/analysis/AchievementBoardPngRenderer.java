package gg.popn.http.analysis;

import gg.popn.application.tier.AchievementBoard;
import gg.popn.application.tier.AchievementBoardImagePort;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Matches the production CPI/SPI map: white canvas, five banner cards and colored bands. */
@Component
public class AchievementBoardPngRenderer implements AchievementBoardImagePort {
    private static final Color INK = new Color(26,28,32), MUTED = new Color(134,139,148), BORDER = new Color(220,222,227);
    private static final String[] ACCENTS = {"#ef4444", "#f97316", "#d946ef", "#8b5cf6", "#3b82f6", "#06b6d4", "#22c55e", "#eab308"};
    private final TierListImageRenderer tierRenderer;
    public AchievementBoardPngRenderer(TierListImageRenderer tierRenderer) { this.tierRenderer = tierRenderer; }

    @Override public byte[] render(AchievementBoard board, int page) {
        var charts = board.page(page);
        var images = tierRenderer.loadAchievementJackets(charts.stream().map(AchievementBoard.Chart::jacketUrl).toList());
        int rows = Math.max(1, (charts.size() + 4) / 5), height = 220 + rows * 123;
        var canvas = new BufferedImage(1160, height, BufferedImage.TYPE_INT_RGB);
        var g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.WHITE); g.fillRect(0, 0, 1160, height);
            text(g, "popn.gg  /  ACHIEVEMENT MAP", 28, 43, 13, MUTED, true);
            text(g, "Lv" + board.level() + " " + (board.metric() == AchievementBoard.Metric.S ? "S" : "퍼펙트")
                    + " 달성자 서열표", 28, 85, 28, INK, true);
            text(g, "달성자 많은 순  ·  " + board.charts().size() + " CHARTS  ·  " + page + " / " + board.pages(),
                    28, 115, 13, MUTED, false);
            text(g, "S ≥ 98,000 (퍼펙트 포함)  /  PERFECT = 금별  /  공개 유저 · BOT 제외 · 역대 기록", 28, 142, 12, MUTED, false);
            g.setColor(BORDER); g.drawLine(28, 157, 1132, 157);
            text(g, "동수는 같은 순위 · 채보 ID 순", 28, 183, 11, MUTED, false);
            for (int row = 0; row < rows; row++) {
                int y = 202 + row * 123;
                Color accent = Color.decode(ACCENTS[row % ACCENTS.length]);
                int start = (page - 1) * AchievementBoard.PAGE_SIZE + row * 5;
                text(g, charts.isEmpty() ? "—" : String.format("%03d–%03d", start + 1, Math.min(start + 5, board.charts().size())),
                        28, y + 24, 14, accent, true);
                text(g, board.metric().name(), 28, y + 44, 10, MUTED, true);
                for (int col = 0; col < 5 && row * 5 + col < charts.size(); col++) {
                    var chart = charts.get(row * 5 + col);
                    int x = 128 + col * 201, width = 197;
                    var jacket = chart.jacketUrl() == null ? null : images.get(chart.jacketUrl());
                    g.setColor(new Color(238,239,241)); g.fillRect(x + 1, y + 1, width - 2, 46);
                    if (jacket != null) {
                        double scale = Math.min((width - 2.0) / jacket.getWidth(), 46.0 / jacket.getHeight());
                        int w = (int) (jacket.getWidth() * scale), h = (int) (jacket.getHeight() * scale);
                        g.drawImage(jacket, x + (width - w) / 2, y + 1 + (46 - h) / 2, w, h, null);
                    } else fit(g, chart.genre(), x + 7, y + 28, 11, MUTED, width - 14);
                    Color difficulty = switch (chart.difficulty()) {
                        case "EX" -> new Color(227,91,121);
                        case "H", "HYPER" -> new Color(217,157,32);
                        case "N", "NORMAL" -> new Color(85,185,120);
                        default -> new Color(69,189,232);
                    };
                    g.setColor(new Color(248,249,251)); g.fillRect(x + 1, y + 47, width - 2, 24);
                    g.setColor(difficulty); g.fillRect(x + 1, y + 47, 40, 24);
                    String label = switch (chart.difficulty()) { case "HYPER" -> "H"; case "NORMAL" -> "N"; case "LIGHT" -> "L"; default -> chart.difficulty(); };
                    text(g, label + " " + board.level(), x + 4, y + 63, 9, Color.WHITE, true);
                    fit(g, (chart.upper() ? "UP · " : "") + chart.title(), x + 46, y + 63, 10, INK, width - 52);
                    int count = count(board, chart), rank = 1;
                    for (var other : board.charts()) { if (count(board, other) <= count) break; rank++; }
                    text(g, "#" + rank, x + 7, y + 92, 11, accent, true);
                    text(g, "S " + chart.sCount() + "명 / PF " + chart.perfectCount() + "명", x + 43, y + 92, 10, INK, true);
                    g.setColor(BORDER); g.drawRect(x, y, width - 1, 110);
                }
            }
            if (charts.isEmpty()) text(g, "해당 레벨의 채보가 없습니다.", 160, 255, 18, INK, false);
            text(g, "popn.gg / " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'KST'")
                    .withZone(ZoneId.of("Asia/Seoul")).format(board.asOf()), 28, height - 8, 10, MUTED, false);
        } finally { g.dispose(); }
        try {
            var output = new ByteArrayOutputStream();
            if (!ImageIO.write(canvas, "png", output)) throw new IllegalStateException("PNG writer unavailable.");
            return output.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Image rendering failed.", e); }
    }
    private static int count(AchievementBoard board, AchievementBoard.Chart chart) {
        return board.metric() == AchievementBoard.Metric.S ? chart.sCount() : chart.perfectCount();
    }
    private static void text(Graphics2D g, String text, int x, int y, int size, Color color, boolean bold) {
        g.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        g.setColor(color); g.drawString(text, x, y);
    }
    private static void fit(Graphics2D g, String text, int x, int y, int size, Color color, int width) {
        String value = text == null ? "" : text;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
        if (g.getFontMetrics().stringWidth(value) > width) {
            while (!value.isEmpty() && g.getFontMetrics().stringWidth(value + "…") > width)
                value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
            value += "…";
        }
        text(g, value, x, y, size, color, false);
    }
}
