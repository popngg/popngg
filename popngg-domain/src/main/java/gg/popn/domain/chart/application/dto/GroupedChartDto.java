package gg.popn.domain.chart.application.dto;

import gg.popn.domain.common.model.Chart;
import gg.popn.domain.common.model.GroupedChart;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GroupedChartDto {
    ChartDto lightChart;
    ChartDto normalChart;
    ChartDto hyperChart;
    ChartDto exChart;

    public static GroupedChartDto from(GroupedChart groupedChart) {
        return GroupedChartDto.builder()
                .lightChart(ChartDto.from(groupedChart.getLightChart()))
                .normalChart(ChartDto.from(groupedChart.getNormalChart()))
                .hyperChart(ChartDto.from(groupedChart.getHyperChart()))
                .exChart(ChartDto.from(groupedChart.getExChart()))
                .build();
    }
}
