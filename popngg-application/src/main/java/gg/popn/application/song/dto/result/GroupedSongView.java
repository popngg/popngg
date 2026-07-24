package gg.popn.application.song.dto.result;

import java.util.List;

public record GroupedSongView(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl,
        List<SongChartView> charts
) {
}
