package gg.popn.application.song.dto.command;

import java.util.List;

public record CreateSongCommand(
        String songHash,
        String genreName,
        String songName,
        String artistName,
        int version,
        String jacketUrl,
        List<CreateChartCommand> charts
) {
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
