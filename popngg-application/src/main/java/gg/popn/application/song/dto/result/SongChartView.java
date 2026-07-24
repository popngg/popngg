package gg.popn.application.song.dto.result;

public record SongChartView(
        long chartId,
        int level,
        int difficulty,
        String difficultyLabel,
        int chartVersion,
        boolean isUpper,
        boolean hasStrictGauge,
        boolean hasStrictJudgement
) {
}
