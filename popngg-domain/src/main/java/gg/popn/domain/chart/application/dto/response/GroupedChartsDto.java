package gg.popn.domain.chart.application.dto.response;

import gg.popn.domain.chart.application.dto.GroupedChartDto;
import gg.popn.domain.chart.model.GroupedChart;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class GroupedChartsDto {
    List<GroupedChartDto> groupedCharts;

    public static GroupedChartsDto from(List<GroupedChart> groupedCharts) {
        return GroupedChartsDto.builder()
                .groupedCharts(groupedCharts.stream()
                        .map(GroupedChartDto::from)
                        .toList())
                .build();
    }
}
