package gg.popn.http.chart.mapper;

import gg.popn.application.chart.dto.command.CreateChartCommand;
import gg.popn.application.chart.dto.command.FindChartCommand;
import gg.popn.application.chart.dto.command.FindGroupedChartCommand;
import gg.popn.domain.chart.model.field.*;
import gg.popn.http.chart.request.CreateChartRequest;

public final class ChartAssembler {
    private ChartAssembler() {}

    public static CreateChartCommand toCreateCommand(CreateChartRequest req) {
        return CreateChartCommand.builder()
                .genreName(GenreName.of(req.getGenreName()))
                .songName( SongName.of(req.getSongName()))
                .levels(req.getLevels().stream()
                        .map(Level::of)
                        .toList())
                .version(Version.of(req.getVersion()))
                .isUpper(IsUpper.of(req.getIsUpper()))
                .build();
    }

    public static FindChartCommand toFindCommand(String songHash, Integer difficulty) {
        return FindChartCommand.builder()
                .songHash(SongHash.of(songHash))
                .difficulty(Difficulty.of(difficulty))
                .build();
    }

    public static FindGroupedChartCommand toFindGroupCommand(String songHash) {
        return FindGroupedChartCommand.builder()
                .songHash(SongHash.of(songHash))
                .build();
    }


}