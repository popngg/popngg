package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.ChartDetailView;

public record ChartDetailResponse(
        SongMetadataResponse song,
        ChartMetadataResponse chart
) {
    public static ChartDetailResponse from(ChartDetailView view) {
        return new ChartDetailResponse(SongMetadataResponse.from(view.song()),
                ChartMetadataResponse.from(view.chart()));
    }
}
