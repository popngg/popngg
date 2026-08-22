package gg.popn.http.playdata.request;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportPlaydataRequest(
        @Valid ProfileSnapshot profile,
        @NotEmpty @Size(max = 10_000) List<@Valid Row> rows
) {
    public ImportPlaydataCommand toCommand(String poptomoId) {
        return new ImportPlaydataCommand(poptomoId,
                profile == null ? null : profile.toCommand(),
                rows.stream().map(Row::toCommand).toList());
    }

    public record ProfileSnapshot(
            @Size(max = 64) String userName,
            @Size(max = 128) String characterName,
            @Min(0) Integer normalCredit,
            @Min(0) Integer extraCredit,
            @Min(0) Integer timePlay10Credit,
            @Min(0) Integer timePlay16Credit
    ) {
        ImportPlaydataCommand.ProfileSnapshot toCommand() {
            return new ImportPlaydataCommand.ProfileSnapshot(userName, characterName,
                    normalCredit, extraCredit, timePlay10Credit, timePlay16Credit);
        }
    }

    public record Row(
            @Min(1) Long chartId,
            @Min(1) Long songId,
            Integer difficultyCode,
            Boolean isUpper,
            @Size(max = 64) String songHash,
            @Size(max = 255) String songName,
            @Size(max = 255) String genreName,
            @NotNull @Min(0) @Max(100_000) Integer score,
            @NotNull Integer rankCode,
            @NotNull @Min(1) @Max(13) Integer medalCode
    ) {
        ImportPlaydataCommand.Row toCommand() {
            return new ImportPlaydataCommand.Row(chartId, songId, difficultyCode, isUpper,
                    songHash, songName, genreName, score, rankCode, medalCode);
        }
    }
}
