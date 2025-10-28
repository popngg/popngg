package gg.popn.application.chart.port.in;

import gg.popn.application.chart.dto.response.GroupedChartsDto;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;
import gg.popn.application.chart.dto.ChartDto;

public interface GetChartUseCase {
    ChartDto getChartBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty);

    GroupedChartsDto getAllCharts();
}
