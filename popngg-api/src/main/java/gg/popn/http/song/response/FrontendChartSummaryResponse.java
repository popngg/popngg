package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.ChartMetadataView;
import gg.popn.application.song.dto.result.SongChartView;

public record FrontendChartSummaryResponse(
        long chartId,
        int difficulty,
        int level,
        int chartVersion,
        boolean hasStrictJudgement,
        boolean hasStrictGauge
) {
    public static FrontendChartSummaryResponse from(SongChartView view) {
        return new FrontendChartSummaryResponse(view.chartId(), view.difficulty(), view.level(),
                view.chartVersion(), view.hasStrictJudgement(), view.hasStrictGauge());
    }

    public static FrontendChartSummaryResponse from(ChartMetadataView view) {
        return new FrontendChartSummaryResponse(view.chartId(), view.difficulty().code(), view.level(),
                view.chartVersion(), view.hasStrictJudgement(), view.hasStrictGauge());
    }
}
