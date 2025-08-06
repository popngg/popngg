package gg.popn.infra.converter;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.GroupedChart;
import gg.popn.domain.chart.model.field.*;
import gg.popn.infra.db.entity.ChartEntity;

import java.util.List;

public class ChartConverter {
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
        return null; // TODO: implement
    }
}
