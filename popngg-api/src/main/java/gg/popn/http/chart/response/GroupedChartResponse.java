package gg.popn.http.chart.response;

import gg.popn.application.chart.dto.result.GroupedChartResult;

public record GroupedChartResponse(
        ChartResponse lightChart,
        ChartResponse normalChart,
        ChartResponse hyperChart,
        ChartResponse exChart
) {

    public static GroupedChartResponse from(GroupedChartResult result) {
        return new GroupedChartResponse(
                result.lightChart() != null ? ChartResponse.from(result.lightChart()) : null,
                result.normalChart() != null ? ChartResponse.from(result.normalChart()) : null,
                result.hyperChart() != null ? ChartResponse.from(result.hyperChart()) : null,
                result.exChart() != null ? ChartResponse.from(result.exChart()) : null
        );
    }
}