package gg.popn.application.chart.dto.result;

import gg.popn.domain.chart.model.GroupedChart;
import lombok.Builder;

@Builder
public record GroupedChartResult(ChartResult lightChart, ChartResult normalChart, ChartResult hyperChart,
                                 ChartResult exChart) {
    public static GroupedChartResult from(GroupedChart groupedChart) {
        return GroupedChartResult.builder()
                .lightChart(groupedChart.getLightChart() != null ? ChartResult.from(groupedChart.getLightChart()) : null)
                .normalChart(groupedChart.getNormalChart() != null ? ChartResult.from(groupedChart.getNormalChart()) : null)
                .hyperChart(groupedChart.getHyperChart() != null ? ChartResult.from(groupedChart.getHyperChart()) : null)
                .exChart(groupedChart.getExChart() != null ? ChartResult.from(groupedChart.getExChart()) : null)
                .build();
    }
}
