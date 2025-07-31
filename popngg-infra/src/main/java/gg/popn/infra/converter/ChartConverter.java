package gg.popn.infra.converter;

import gg.popn.domain.common.model.Chart;
import gg.popn.infra.db.entity.ChartEntity;

public class ChartConverter {
    public static Chart toDomain(ChartEntity chartEntity) {
        return Chart.builder()
                .songHash(chartEntity.getSongHash())
                .genreName(chartEntity.getGenreName())
                .songName(chartEntity.getSongName())
                .version(chartEntity.getVersion())
                .difficulty(chartEntity.getDifficulty())
                .level(chartEntity.getLevel())
                .isUpper(chartEntity.getIsUpper())
                .build();
    }

    public static ChartEntity toEntity(Chart chart) {
        return ChartEntity.builder()
                .songHash(chart.getSongHash())
                .genreName(chart.getGenreName())
                .songName(chart.getSongName())
                .version(chart.getVersion())
                .difficulty(chart.getDifficulty())
                .level(chart.getLevel())
                .isUpper(chart.getIsUpper())
                .build();
    }
}
