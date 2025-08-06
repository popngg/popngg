package gg.popn.domain.chart.application.service;

import gg.popn.domain.chart.application.dto.ChartDto;
import gg.popn.domain.chart.application.service.validator.ChartValidator;
import gg.popn.domain.chart.model.Difficulty;
import gg.popn.domain.chart.application.port.in.GetChartUseCase;
import gg.popn.domain.chart.application.port.out.ChartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetChartService implements GetChartUseCase {
    private final ChartValidator chartValidator;
    private final ChartRepository chartRepository;

    @Override
    public ChartDto getChartBySongHashAndDifficulty(String songHash, Integer difficulty) {
        chartValidator.validate(Difficulty.of(difficulty));

        return ChartDto.from(
                chartRepository.getChartBySongHashAndDifficulty(songHash, difficulty)
        );
    }
}
