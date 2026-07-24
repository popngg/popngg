package gg.popn.application.chart.dto.result;

import gg.popn.domain.chart.model.Chart;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class ChartListResult {
    List<ChartResult> charts;

    public static ChartListResult from(List<Chart> charts) {
        return ChartListResult.builder()
                .charts(charts.stream()
                        .map(ChartResult::from)
                        .toList())
                .build();
    }
}
