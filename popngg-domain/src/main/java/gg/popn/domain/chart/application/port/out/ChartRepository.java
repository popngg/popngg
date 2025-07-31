package gg.popn.domain.chart.application.port.out;

import gg.popn.domain.common.model.Chart;

import java.util.List;

public interface ChartRepository {
    List<Chart> getChartsByLevel(Integer level);
}
