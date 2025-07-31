package gg.popn.domain.chart.application.service;

import gg.popn.domain.chart.application.dto.GetChartResponse;
import gg.popn.domain.chart.application.service.validator.ChartLevelValidator;
import gg.popn.domain.common.model.Chart;
import gg.popn.domain.chart.application.port.in.GetChartUseCase;
import gg.popn.domain.chart.application.port.out.ChartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetChartService implements GetChartUseCase {
    private final ChartLevelValidator chartLevelValidator;
    private final ChartRepository chartRepository;

    @Override
    public GetChartResponse getChartsByLevel(Integer level) {
        chartLevelValidator.validate(level);

        List<Chart> charts = chartRepository.getChartsByLevel(level);
        return GetChartResponse.from(charts);
    }
}
