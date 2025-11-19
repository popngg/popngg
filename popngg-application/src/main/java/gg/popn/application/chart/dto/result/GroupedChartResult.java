package gg.popn.application.chart.dto.result;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.GroupedChart;
import lombok.Builder;

@Builder
public record GroupedChartResult(
        ChartResult lightChart,
        ChartResult normalChart,
        ChartResult hyperChart,
        ChartResult exChart
) {
    public static GroupedChartResult from(GroupedChart groupedChart) {
        return GroupedChartResult.builder()
                .lightChart(toChartResultOrNull(groupedChart.getLightChart()))
                .normalChart(toChartResultOrNull(groupedChart.getNormalChart()))
                .hyperChart(toChartResultOrNull(groupedChart.getHyperChart()))
                .exChart(toChartResultOrNull(groupedChart.getExChart()))
                .build();
    }

    private static ChartResult toChartResultOrNull(Chart chart) {
        return chart != null ? ChartResult.from(chart) : null;
    }
}