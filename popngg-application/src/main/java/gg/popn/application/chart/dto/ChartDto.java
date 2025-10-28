package gg.popn.application.chart.dto;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.*;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ChartDto {
    SongHash songHash;
    GenreName genreName;
    SongName songName;
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
