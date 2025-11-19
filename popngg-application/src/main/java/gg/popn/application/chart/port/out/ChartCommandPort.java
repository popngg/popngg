package gg.popn.application.chart.port.out;

import gg.popn.domain.chart.model.Chart;

public interface  ChartCommandPort {
    void save(Chart chart);
}
