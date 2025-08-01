package gg.popn.domain.chart.application.port.in;

import gg.popn.domain.chart.application.dto.ChartDto;
import gg.popn.domain.chart.application.dto.GetChartResponse;

public interface GetChartUseCase {
    GetChartResponse getChartsByLevel(Integer level);

    ChartDto getChartBySongHashAndDifficulty(String songHash, Integer difficulty);
}
