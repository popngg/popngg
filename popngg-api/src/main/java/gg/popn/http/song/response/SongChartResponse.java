package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.SongChartView;

public record SongChartResponse(
        long chartId,
        int level,
        int difficulty,
        String difficultyLabel,
        int chartVersion,
        boolean isUpper,
        boolean hasStrictGauge,
        boolean hasStrictJudgement
) {
    public static SongChartResponse from(SongChartView view) {
        return new SongChartResponse(view.chartId(), view.level(), view.difficulty(),
                view.difficultyLabel(), view.chartVersion(), view.isUpper(),
                view.hasStrictGauge(), view.hasStrictJudgement());
    }
}
