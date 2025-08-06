package gg.popn.domain.chart.application.port.out;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;

public interface ChartRepository {
    Chart getChartBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty);
}
