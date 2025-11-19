package gg.popn.http.chart.response;

import gg.popn.application.chart.dto.result.CreateChartResult;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.Level;

import java.util.List;

public record CreateChartResponse(
        String songHash,
        List<Integer> createdLevels,
        int createdCount
) {
    public static CreateChartResponse from(CreateChartResult result) {
        return new CreateChartResponse(
                result.songHash().getValue(),
                result.createdLevels().stream()
                        .map(Level::getValue)
                        .toList(),
                result.createdCount()
        );
    }
}