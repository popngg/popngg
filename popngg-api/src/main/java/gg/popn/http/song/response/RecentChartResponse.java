package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.GroupedSongView;

import java.util.List;
import java.time.Instant;

public record RecentChartResponse(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String bannerUrl,
        Instant createdAt,
        List<SongChartResponse> charts
) {
    public static RecentChartResponse from(GroupedSongView view) {
        return new RecentChartResponse(view.songId(), view.songHash(), view.genreName(),
                view.songName(), view.artistName(), view.version(), view.jacketUrl(), view.createdAt(),
                view.charts().stream().map(SongChartResponse::from).toList());
    }
}
