package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.SongDetailView;

import java.util.List;

public record SongDetailResponse(
        SongMetadataResponse song,
        List<ChartMetadataResponse> charts
) {
    public static SongDetailResponse from(SongDetailView view) {
        return new SongDetailResponse(SongMetadataResponse.from(view.song()),
                view.charts().stream().map(ChartMetadataResponse::from).toList());
    }
}
