package gg.popn.application.chart.service;

import gg.popn.application.chart.dto.command.FindChartCommand;
import gg.popn.application.chart.dto.result.ChartResult;
import gg.popn.application.chart.port.in.FindChartUseCase;
import gg.popn.application.chart.port.out.ChartQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindChartService implements FindChartUseCase {

    private final ChartQueryPort chartQueryPort;

    @Override
    public ChartResult execute(FindChartCommand cmd) {
        return ChartResult.from(
                chartQueryPort.findBySongHashAndDifficulty(cmd.songHash(), cmd.difficulty())
        );
    }
}
