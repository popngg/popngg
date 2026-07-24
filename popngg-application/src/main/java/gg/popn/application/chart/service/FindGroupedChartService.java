package gg.popn.application.chart.service;

import gg.popn.application.chart.dto.command.FindGroupedChartCommand;
import gg.popn.application.chart.dto.result.GroupedChartResult;
import gg.popn.application.chart.port.in.FindGroupedChartUseCase;
import gg.popn.application.chart.port.out.ChartQueryPort;
import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.GroupedChart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindGroupedChartService implements FindGroupedChartUseCase {
    private final ChartQueryPort chartQueryPort;

    @Override
    public GroupedChartResult execute(FindGroupedChartCommand cmd){
        List<Chart> charts = chartQueryPort.findListBySongHash(cmd.songHash());
        GroupedChart.GroupedChartBuilder builder = GroupedChart.builder();

        for (Chart chart : charts) {
            int diff = chart.getDifficulty().getValue();
            switch (diff) {
                case 1 -> builder.lightChart(chart);
                case 2 -> builder.normalChart(chart);
                case 3 -> builder.hyperChart(chart);
                case 4 -> builder.exChart(chart);
            }
        }

        GroupedChart groupedChart = builder.build();

        return GroupedChartResult.from(groupedChart);
    }

}
