package gg.popn.domain.chart.application.port.in;

import gg.popn.domain.chart.application.dto.GetChartResponse;

public interface GetChartUseCase {
    GetChartResponse getChartsByLevel(Integer level);
}
