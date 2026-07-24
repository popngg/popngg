package gg.popn.application.chart.dto.result;

import gg.popn.domain.chart.model.GroupedChart;
import lombok.Builder;

import java.util.List;

@Builder
public record GroupedChartListResult(List<GroupedChartResult> groupedCharts) {
    public static GroupedChartListResult from(List<GroupedChart> groupedCharts) {
        return GroupedChartListResult.builder()
                .groupedCharts(groupedCharts.stream()
                        .map(GroupedChartResult::from)
                        .toList())
                .build();
    }
}
