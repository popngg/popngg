package gg.popn.http.chart.response;

import gg.popn.application.chart.dto.result.ChartResult;

public record ChartResponse(
        String songHash,
        String genreName,
        String songName,
        Integer version,
        Integer difficulty,
        Integer level,
        Integer isUpper
) {
    public static ChartResponse from(ChartResult result) {
        return new ChartResponse(
                result.songHash().getValue(),
                result.genreName().getValue(),
                result.songName().getValue(),
                result.version().getValue(),
                result.difficulty().getValue(),
                result.level().getValue(),
                result.isUpper().getValue()
        );
    }
}