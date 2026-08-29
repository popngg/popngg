package gg.popn.application.song.dto.command;

import java.util.List;
import java.time.Instant;

public record CreateSongCommand(
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl,
        Instant createdAt,
        List<CreateChartCommand> charts
) {
    public CreateSongCommand(String songHash, String genreName, String songName,
                             String artistName, int version, String jacketUrl,
                             List<CreateChartCommand> charts) {
        this(songHash, genreName, songName, artistName, version, jacketUrl, null, charts);
    }
    public record CreateChartCommand(
            int difficulty,
            int level,
            int chartVersion,
            boolean isUpper,
            boolean hasStrictGauge,
            boolean hasStrictJudgement
    ) {
    }
}
