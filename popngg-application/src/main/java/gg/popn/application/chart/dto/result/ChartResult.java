package gg.popn.application.chart.dto.result;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.*;
import lombok.Builder;
import lombok.Value;

public record ChartResult(
        SongHash songHash,
        GenreName genreName,
        SongName songName,
        Version version,
        Difficulty difficulty,
        Level level,
        IsUpper isUpper
) {
    public static ChartResult from(Chart chart) {
        return new ChartResult(
                chart.getSongHash(),
                chart.getGenreName(),
                chart.getSongName(),
                chart.getVersion(),
                chart.getDifficulty(),
                chart.getLevel(),
                chart.getIsUpper()
        );
    }
}
