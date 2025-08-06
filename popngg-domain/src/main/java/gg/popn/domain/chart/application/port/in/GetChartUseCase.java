package gg.popn.domain.chart.application.port.in;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;
import gg.popn.domain.chart.application.dto.ChartDto;

public interface GetChartUseCase {
    ChartDto getChartBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty);
}
