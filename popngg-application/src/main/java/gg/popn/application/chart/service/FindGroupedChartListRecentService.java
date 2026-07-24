package gg.popn.application.chart.service;

import gg.popn.application.chart.dto.result.GroupedChartListResult;
import gg.popn.application.chart.port.in.FindGroupedChartListRecentUseCase;
import gg.popn.application.chart.port.out.ChartQueryPort;
import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.GroupedChart;
import gg.popn.domain.chart.model.field.SongHash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindGroupedChartListRecentService implements FindGroupedChartListRecentUseCase {
    private final ChartQueryPort chartQuery;
    private final Integer LIMIT =5;

    @Override
    public GroupedChartListResult execute(){


        List<Chart> charts = chartQuery.findRecentCharts(LIMIT);
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

        return GroupedChartListResult.from(groupedCharts);
    }
}
