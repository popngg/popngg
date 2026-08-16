package gg.popn.infra.db.mapper;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.GroupedChart;
import gg.popn.domain.chart.model.field.*;
import gg.popn.infra.db.entity.ChartEntity;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChartMapper {
    public static Chart toDomain(ChartEntity chartEntity) {
        return Chart.builder()
                .songHash(SongHash.of(chartEntity.getSongHash()))
                .genreName(GenreName.of(chartEntity.getGenreName()))
                .songName(SongName.of(chartEntity.getSongName()))
                .version(Version.of(chartEntity.getVersion()))
                .difficulty(Difficulty.of(chartEntity.getDifficulty()))
                .level(Level.of(chartEntity.getLevel()))
                .isUpper(IsUpper.of(chartEntity.getIsUpper()))
                .build();
    }

    public static ChartEntity toEntity(Chart chart) {
        return ChartEntity.builder()
                .songHash(chart.getSongHash().getValue())
                .genreName(chart.getGenreName().getValue())
                .songName(chart.getSongName().getValue())
                .version(chart.getVersion().getValue())
                .difficulty(chart.getDifficulty().getValue())
                .level(chart.getLevel().getValue())
                .isUpper(chart.getIsUpper().getValue())
                .build();
    }

    public static List<GroupedChart> toGroupedChart(List<Chart> charts) {
        if (charts == null || charts.isEmpty()) {
            return List.of();
        }
        Map<String, GroupedChart.GroupedChartBuilder> grouped = new LinkedHashMap<>();
        for (Chart chart : charts) {
            var builder = grouped.computeIfAbsent(
                    chart.getSongHash().getValue(), ignored -> GroupedChart.builder());
            switch (chart.getDifficulty().getValue()) {
                case 1 -> builder.lightChart(chart);
                case 2 -> builder.normalChart(chart);
                case 3 -> builder.hyperChart(chart);
                case 4 -> builder.exChart(chart);
                default -> throw new IllegalArgumentException("Unsupported chart difficulty.");
            }
        }
        return grouped.values().stream().map(GroupedChart.GroupedChartBuilder::build).toList();
    }
}
