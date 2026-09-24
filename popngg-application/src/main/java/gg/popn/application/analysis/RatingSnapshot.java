package gg.popn.application.analysis;

import java.util.List;

public record RatingSnapshot(
        String snapshotId,
        String generatedAt,
        String modelVersion,
        String modelStatus,
        int minimumPlayers,
        List<ChartRating> charts
) {
    public record ChartRating(
            long chartId, long songId, String songName, String genreName, String jacketUrl,
            int level, int difficulty, boolean upper,
            String cpiStatus, Double cpi, int cpiSampleCount, int clearCount, Double clearRate,
            String spiStatus, Double spi, int spiSampleCount, Double averageScore, Double medianScore
    ) {}
}
