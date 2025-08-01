package gg.popn.domain.chart.application.dto;

import gg.popn.domain.chart.model.Difficulty;
import gg.popn.domain.chart.model.IsUpper;
import gg.popn.domain.chart.model.Level;
import gg.popn.domain.chart.model.Version;
import gg.popn.domain.common.model.Chart;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ChartDto {
    String songHash;
    String genreName;
    String songName;
    Version version;
    Difficulty difficulty;
    Level level;
    IsUpper isUpper;

    public static ChartDto from(Chart chart) {
        return ChartDto.builder()
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
