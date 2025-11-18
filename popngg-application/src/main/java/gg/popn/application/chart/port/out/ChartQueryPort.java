package gg.popn.application.chart.port.out;

import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;

import java.util.List;

public interface ChartQueryPort {
    Chart findBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty);
    List<Chart> findListBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty);
    List<Chart> findAllCharts();
}