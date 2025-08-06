package gg.popn.domain.chart.application.service;

import gg.popn.domain.chart.application.dto.ChartDto;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.application.port.in.GetChartUseCase;
import gg.popn.domain.chart.application.port.out.ChartRepository;
import gg.popn.domain.chart.model.field.SongHash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetChartService implements GetChartUseCase {
    private final ChartRepository chartRepository;

    @Override
    public ChartDto getChartBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty) {

        return ChartDto.from(
                chartRepository.getChartBySongHashAndDifficulty(songHash, difficulty)
        );
    }
}
