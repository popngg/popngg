package gg.popn.infra.db.repository;

import gg.popn.domain.common.exception.ChartNotFoundException;
import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.application.port.out.ChartRepository;
import gg.popn.infra.converter.ChartConverter;
import gg.popn.infra.db.jpa.ChartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChartRepositoryImpl implements ChartRepository {
    private final ChartJpaRepository chartJpaRepository;

    @Override
    public Chart getChartBySongHashAndDifficulty(String songHash, Integer difficulty) {
        return chartJpaRepository.findAllBySongHashAndDifficulty(songHash, difficulty)
                .stream()
                .filter(chartEntity -> !chartEntity.getIsDeleted().equals(1))
                .findFirst()
                .map(ChartConverter::toDomain)
                .orElseThrow(ChartNotFoundException::new);
    }
}
