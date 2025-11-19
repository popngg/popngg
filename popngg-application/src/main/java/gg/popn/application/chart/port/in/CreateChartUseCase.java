package gg.popn.application.chart.port.in;


import gg.popn.application.chart.dto.command.CreateChartCommand;
import gg.popn.application.chart.dto.result.CreateChartResult;

public interface CreateChartUseCase {
    CreateChartResult execute(CreateChartCommand cmd) throws Exception;
}
