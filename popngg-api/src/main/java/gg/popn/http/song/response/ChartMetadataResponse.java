package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.ChartMetadataView;

public record ChartMetadataResponse(
        long chartId,
        DifficultyResponse difficulty,
        int level,
        int chartVersion,
        boolean isUpper,
        boolean hasStrictGauge,
        boolean hasStrictJudgement,
        boolean isDeleted
) {
    public static ChartMetadataResponse from(ChartMetadataView view) {
        return new ChartMetadataResponse(view.chartId(), DifficultyResponse.from(view.difficulty()),
                view.level(), view.chartVersion(), view.isUpper(), view.hasStrictGauge(),
                view.hasStrictJudgement(), view.isDeleted());
    }
}
