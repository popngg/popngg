package gg.popn.application.chart.dto.response;

import gg.popn.application.chart.dto.ChartDto;
import gg.popn.domain.chart.model.Chart;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class ChartsDto {
    List<ChartDto> charts;

    public static ChartsDto from(List<Chart> charts) {
        return ChartsDto.builder()
                .charts(charts.stream()
                        .map(ChartDto::from)
                        .toList())
                .build();
    }
}
