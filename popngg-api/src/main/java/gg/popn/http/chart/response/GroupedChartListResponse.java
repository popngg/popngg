package gg.popn.http.chart.response;

import gg.popn.application.chart.dto.result.GroupedChartListResult;

import java.util.List;

public record GroupedChartListResponse(
        List<GroupedChartResponse> groupedCharts
) {

    public static GroupedChartListResponse from(GroupedChartListResult result) {
        return new GroupedChartListResponse(
                result.groupedCharts().stream()
                        .map(GroupedChartResponse::from)
                        .toList()
        );
    }
}
