package gg.popn.application.chart.service;

import gg.popn.application.chart.dto.ChartDto;
import gg.popn.application.chart.dto.response.GroupedChartsDto;
import gg.popn.application.chart.port.in.GetChartUseCase;
import gg.popn.application.chart.port.out.ChartQueryPort;
import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.GroupedChart;
import gg.popn.domain.chart.model.field.Difficulty;

import gg.popn.domain.chart.model.field.SongHash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetChartService implements GetChartUseCase {
    private final ChartQueryPort chartQuery;

    @Override
    public ChartDto getChartBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty) {

        return ChartDto.from(
                chartQuery.getChartBySongHashAndDifficulty(songHash, difficulty)
        );
    }

    @Override
    public GroupedChartsDto getAllCharts() {
        List<Chart> charts = chartQuery.getAllCharts();
        Map<SongHash, List<Chart>> groupedBySongHash = charts.stream()
                .collect(Collectors.groupingBy(Chart::getSongHash));

        List<GroupedChart> groupedCharts = new ArrayList<>();
        for (List<Chart> chartList : groupedBySongHash.values()) {
            GroupedChart.GroupedChartBuilder builder = GroupedChart.builder();
            for (Chart chart : chartList) {
                switch (chart.getDifficulty().getValue()) {
                    case 1:
                        builder.lightChart(chart);
                        break;
                    case 2:
                        builder.normalChart(chart);
                        break;
                    case 3:
                        builder.hyperChart(chart);
                        break;
                    case 4:
                        builder.exChart(chart);
                        break;
                }
            }
            groupedCharts.add(builder.build());
        }

        return GroupedChartsDto.from(groupedCharts);
    }
}
