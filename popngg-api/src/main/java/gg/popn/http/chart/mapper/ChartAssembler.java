package gg.popn.http.chart.mapper;

import gg.popn.application.chart.dto.command.CreateChartCommand;
import gg.popn.application.chart.dto.command.FindChartCommand;
import gg.popn.application.chart.dto.result.ChartResult;
import gg.popn.domain.chart.model.field.*;
import gg.popn.http.chart.request.CreateChartRequest;
import gg.popn.http.chart.response.ChartResponse;

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


}