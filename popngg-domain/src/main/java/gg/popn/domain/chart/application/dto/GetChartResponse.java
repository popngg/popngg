package gg.popn.domain.chart.application.dto;

import gg.popn.domain.common.model.Chart;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class GetChartResponse {
    List<ChartDto> charts;

    public static GetChartResponse from(List<Chart> charts) {
        return GetChartResponse.builder()
                .charts(charts.stream()
                        .map(ChartDto::from)
                        .toList())
                .build();
    }
}
