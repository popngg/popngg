package gg.popn.application.chart.dto.command;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;
import lombok.Builder;

@Builder
public record FindChartCommand(SongHash songHash, Difficulty difficulty) {
}
