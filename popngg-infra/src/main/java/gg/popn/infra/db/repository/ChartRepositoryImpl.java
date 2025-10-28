package gg.popn.infra.db.repository;

import gg.popn.application.chart.port.out.ChartRepository;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;

import gg.popn.domain.common.exception.ChartNotFoundException;
import gg.popn.domain.chart.model.Chart;
import gg.popn.infra.db.entity.ChartEntity;
import gg.popn.infra.db.mapper.ChartMapper;
import gg.popn.infra.db.jpa.ChartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChartRepositoryImpl implements ChartRepository {
    private final ChartJpaRepository chartJpaRepository;

    @Override
    public Chart getChartBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty) {
        return chartJpaRepository.findAllBySongHashAndDifficulty(songHash.getValue(), difficulty.getDifficulty())
                .stream()
                .filter(chartEntity -> !chartEntity.getIsDeleted().equals(1))
                .findFirst()
                .map(ChartMapper::toDomain)
                .orElseThrow(ChartNotFoundException::new);
    }

    @Override
    public List<Chart> getAllCharts() {
        return chartJpaRepository.findAll().stream()
                .filter(chartEntity -> !chartEntity.getIsDeleted().equals(1))
                .map(ChartMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chart> getChartsBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty ){
        return chartJpaRepository.findAllBySongHashAndDifficulty(songHash.getValue(), difficulty.getDifficulty())
                .stream()
                .filter(chartEntity -> Objects.equals(chartEntity.getIsDeleted(), 1))
                .map(ChartMapper::toDomain)
                .toList();

    }

    @Override
    public void save(Chart chart) {
        ChartEntity chartEntity = ChartMapper.toEntity(chart);
        chartJpaRepository.save(chartEntity);
    }
}
