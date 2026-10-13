package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.GroupedSongView;

import java.util.List;
import java.time.Instant;

public record GroupedSongResponse(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl,
        Instant createdAt,
        List<SongChartResponse> charts
) {
    public static GroupedSongResponse from(GroupedSongView view) {
        return new GroupedSongResponse(view.songId(), view.songHash(), view.genreName(),
                view.songName(), view.artistName(), view.version(), view.jacketUrl(), view.createdAt(),
                view.charts().stream().map(SongChartResponse::from).toList());
    }
}
