package gg.popn.domain.chart.application.dto;

import gg.popn.domain.common.model.Chart;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ChartDto {
    String songHash;
    String genreName;
    String songName;
    Integer version;
    Integer difficulty;
    Integer level;
    Integer isUpper;

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
