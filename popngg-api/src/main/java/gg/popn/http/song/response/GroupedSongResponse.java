package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.GroupedSongView;

import java.util.List;

public record GroupedSongResponse(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl,
        List<SongChartResponse> charts
) {
    public static GroupedSongResponse from(GroupedSongView view) {
        return new GroupedSongResponse(view.songId(), view.songHash(), view.genreName(),
                view.songName(), view.artistName(), view.version(), view.jacketUrl(),
                view.charts().stream().map(SongChartResponse::from).toList());
    }
}
