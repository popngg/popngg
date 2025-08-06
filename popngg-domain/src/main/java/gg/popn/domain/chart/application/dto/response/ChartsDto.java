package gg.popn.domain.chart.application.dto.response;

import gg.popn.domain.chart.application.dto.ChartDto;
import gg.popn.domain.common.model.Chart;
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
