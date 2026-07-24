package gg.popn.application.chart.port.in;

import gg.popn.application.chart.dto.command.FindChartCommand;
import gg.popn.application.chart.dto.result.ChartResult;

public interface FindChartUseCase {
    ChartResult execute(FindChartCommand cmd);
}
