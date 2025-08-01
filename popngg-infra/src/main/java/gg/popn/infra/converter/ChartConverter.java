package gg.popn.infra.converter;

import gg.popn.domain.chart.model.Difficulty;
import gg.popn.domain.chart.model.IsUpper;
import gg.popn.domain.chart.model.Level;
import gg.popn.domain.chart.model.Version;
import gg.popn.domain.common.model.Chart;
import gg.popn.infra.db.entity.ChartEntity;

public class ChartConverter {
    public static Chart toDomain(ChartEntity chartEntity) {
        return Chart.builder()
                .songHash(chartEntity.getSongHash())
                .genreName(chartEntity.getGenreName())
                .songName(chartEntity.getSongName())
                .version(Version.of(chartEntity.getVersion()))
                .difficulty(Difficulty.of(chartEntity.getDifficulty()))
                .level(Level.of(chartEntity.getLevel()))
                .isUpper(IsUpper.of(chartEntity.getIsUpper()))
                .build();
    }

    public static ChartEntity toEntity(Chart chart) {
        return ChartEntity.builder()
                .songHash(chart.getSongHash())
                .genreName(chart.getGenreName())
                .songName(chart.getSongName())
                .version(chart.getVersion().getValue())
                .difficulty(chart.getDifficulty().getValue())
                .level(chart.getLevel().getValue())
                .isUpper(chart.getIsUpper().getValue())
                .build();
    }
}
