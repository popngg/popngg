package gg.popn.http.song.request;

import gg.popn.application.song.dto.command.CreateSongCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSongRequest(
        String songHash,
        @NotBlank String genreName,
        @NotBlank String songName,
        String artistName,
        @Min(1) int version,
        String jacketUrl,
        @NotEmpty List<@Valid ChartRequest> charts
) {
    public CreateSongCommand toCommand() {
        return new CreateSongCommand(songHash, genreName, songName, artistName, version, jacketUrl,
                charts.stream().map(ChartRequest::toCommand).toList());
    }

    public record ChartRequest(
            @Min(1) @Max(4) int difficulty,
            @Min(1) @Max(50) int level,
            @Min(1) int chartVersion,
            @NotNull Boolean isUpper,
            @NotNull Boolean hasStrictGauge,
            @NotNull Boolean hasStrictJudgement
    ) {
        CreateSongCommand.CreateChartCommand toCommand() {
            return new CreateSongCommand.CreateChartCommand(difficulty, level, chartVersion,
                    isUpper, hasStrictGauge, hasStrictJudgement);
        }
    }
}
