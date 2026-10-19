package gg.popn.application.tier;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Counts are users per chart, not play counts. S includes perfect records. */
public record AchievementBoard(int level, Metric metric, Instant asOf, List<Chart> charts) {
    public static final int PAGE_SIZE = 80;
    public enum Metric { S, PERFECT }
    public record Chart(long chartId, String title, String genre, String jacketUrl,
                        String difficulty, boolean upper, int sCount, int perfectCount) {}
    public AchievementBoard {
        charts = charts.stream().sorted(Comparator
                .comparingInt((Chart c) -> metric == Metric.S ? c.sCount() : c.perfectCount()).reversed()
                .thenComparingLong(Chart::chartId)).toList();
    }
    public int pages() { return Math.max(1, (charts.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    public List<Chart> page(int page) {
        if (page < 1 || page > pages()) throw new IllegalArgumentException("Invalid page.");
        return charts.subList((page - 1) * PAGE_SIZE, Math.min(page * PAGE_SIZE, charts.size()));
    }
}
