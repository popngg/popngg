package gg.popn.domain.chart.application.port.in;

import gg.popn.domain.chart.application.dto.ChartDto;

public interface GetChartUseCase {
    ChartDto getChartBySongHashAndDifficulty(String songHash, Integer difficulty);
}
