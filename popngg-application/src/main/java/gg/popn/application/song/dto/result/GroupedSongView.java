package gg.popn.application.song.dto.result;

import java.util.List;
import java.time.Instant;

public record GroupedSongView(
        long songId,
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl,
        Instant createdAt,
        List<SongChartView> charts
) {
}
