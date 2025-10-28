package gg.popn.application.chart.port.out;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;

import java.util.List;

public interface ChartRepository {
    Chart getChartBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty);
    List<Chart> getChartsBySongHashAndDifficulty(SongHash songHash, Difficulty difficult);
    List<Chart> getAllCharts();
    void save(Chart chart);
}