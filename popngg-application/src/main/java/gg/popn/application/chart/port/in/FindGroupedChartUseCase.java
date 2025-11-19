package gg.popn.application.chart.port.in;

import gg.popn.application.chart.dto.command.FindGroupedChartCommand;
import gg.popn.application.chart.dto.result.GroupedChartResult;

public interface FindGroupedChartUseCase {
    GroupedChartResult execute(FindGroupedChartCommand cmd);
}
