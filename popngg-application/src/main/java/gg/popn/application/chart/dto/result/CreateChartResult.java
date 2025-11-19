package gg.popn.application.chart.dto.result;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.Level;
import gg.popn.domain.chart.model.field.SongHash;

import java.util.List;

public record CreateChartResult(
        SongHash songHash,
        List<Level> createdLevels,
        int createdCount
) {
}