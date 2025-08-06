package gg.popn.domain.chart.application.port.out;

import gg.popn.domain.chart.model.Chart;

public interface ChartRepository {
    Chart getChartBySongHashAndDifficulty(String songHash, Integer difficulty);
}
