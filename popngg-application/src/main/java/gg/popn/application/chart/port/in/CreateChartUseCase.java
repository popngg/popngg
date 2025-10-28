package gg.popn.application.chart.port.in;


import gg.popn.application.chart.port.in.command.CreateChartCommand;

public interface CreateChartUseCase {
    Integer createChart(CreateChartCommand cmd) throws Exception;
}
