package gg.popn.http.song.response;

import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.dto.result.SongDetailView;

import java.util.List;

/** Response contract consumed by popngg-frontend's pages/charts and pages/chart DTOs. */
public record FrontendChartResponse(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl,
        boolean isUpper,
        List<FrontendChartSummaryResponse> charts
) {
    public static FrontendChartResponse from(GroupedSongView view) {
        var charts = view.charts().stream().map(FrontendChartSummaryResponse::from).toList();
        return new FrontendChartResponse(view.songId(), required(view.songHash()), view.genreName(),
                view.songName(), required(view.artistName()), view.version(), required(view.jacketUrl()),
                upper(view.charts().stream().map(chart -> chart.isUpper()).toList()), charts);
    }

    public static FrontendChartResponse from(SongDetailView view) {
        var song = view.song();
        var activeCharts = view.charts().stream().filter(chart -> !chart.isDeleted()).toList();
        return new FrontendChartResponse(song.songId(), required(song.songHash()), song.genreName(),
                song.songName(), required(song.artistName()), song.version(), required(song.jacketUrl()),
                upper(activeCharts.stream().map(chart -> chart.isUpper()).toList()),
                activeCharts.stream().map(FrontendChartSummaryResponse::from).toList());
    }

    private static boolean upper(List<Boolean> values) {
        return !values.isEmpty() && values.stream().allMatch(Boolean.TRUE::equals);
    }

    private static String required(String value) {
        return value == null ? "" : value;
    }
}
