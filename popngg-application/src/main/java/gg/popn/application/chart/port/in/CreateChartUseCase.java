package gg.popn.application.chart.port.in;


import gg.popn.application.chart.dto.command.CreateChartCommand;

public interface CreateChartUseCase {
    Integer execute(CreateChartCommand cmd) throws Exception;
}
