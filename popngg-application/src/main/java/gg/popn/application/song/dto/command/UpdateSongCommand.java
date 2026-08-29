package gg.popn.application.song.dto.command;

import java.time.Instant;
import java.util.List;

public record UpdateSongCommand(long songId, String genreName, String songName,
        String artistName, int version, String jacketUrl, Instant createdAt,
        List<ChartUpdate> charts) {
    public record ChartUpdate(long chartId, int level, int chartVersion,
            boolean isUpper, boolean hasStrictGauge, boolean hasStrictJudgement) {}
}
