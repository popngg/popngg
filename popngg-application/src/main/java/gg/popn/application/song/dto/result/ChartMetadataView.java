package gg.popn.application.song.dto.result;

public record ChartMetadataView(
        long chartId,
        DifficultyView difficulty,
        int level,
        int chartVersion,
        boolean isUpper,
        boolean hasStrictGauge,
        boolean hasStrictJudgement,
        boolean isDeleted
) {
}
