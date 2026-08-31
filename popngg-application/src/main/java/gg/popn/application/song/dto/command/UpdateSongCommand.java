package gg.popn.application.song.dto.command;

import java.time.Instant;
import java.util.List;

public record UpdateSongCommand(long songId, String genreName, String songName,
        String artistName, int version, String jacketUrl, Instant createdAt,
        List<ChartUpdate> charts) {
    public record ChartUpdate(Long chartId, int difficultyCode, int level, int chartVersion,
            boolean isUpper, boolean hasStrictGauge, boolean hasStrictJudgement) {
        public ChartUpdate(long chartId, int level, int chartVersion, boolean isUpper,
                boolean hasStrictGauge, boolean hasStrictJudgement) {
            this(chartId, 0, level, chartVersion, isUpper, hasStrictGauge, hasStrictJudgement);
        }
    }
}
