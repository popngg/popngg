package gg.popn.infra.db.adapter;

import com.querydsl.jpa.impl.JPAQueryFactory;
import gg.popn.application.chart.port.out.ChartQueryPort;
import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;

import gg.popn.domain.common.exception.ChartNotFoundException;
import gg.popn.domain.chart.model.Chart;
import gg.popn.infra.db.entity.QChartEntity;
import gg.popn.infra.db.mapper.ChartMapper;
import gg.popn.infra.db.jpa.ChartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChartQueryJpaAdapter implements ChartQueryPort {

    private final ChartJpaRepository chartJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Chart findBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty) {
        return chartJpaRepository.findAllBySongHashAndDifficulty(songHash.getValue(), difficulty.getValue())
                .stream()
                .filter(chartEntity -> !chartEntity.getIsDeleted().equals(1))
                .findFirst()
                .map(ChartMapper::toDomain)
                .orElseThrow(ChartNotFoundException::new);
    }

    @Override
    public List<Chart> findAllCharts() {
        return chartJpaRepository.findAll().stream()
                .filter(chartEntity -> !chartEntity.getIsDeleted().equals(1))
                .map(ChartMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chart> findListBySongHash(SongHash songHash) {
        return chartJpaRepository.findAllBySongHash(songHash.getSongHash())
                .stream()
                .filter(chartEntity -> !chartEntity.getIsDeleted().equals(1))
                .map(ChartMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chart> findListBySongHashAndDifficulty(SongHash songHash, Difficulty difficulty ){
        return chartJpaRepository.findAllBySongHashAndDifficulty(songHash.getValue(), difficulty.getDifficulty())
                .stream()
                .filter(chartEntity -> Objects.equals(chartEntity.getIsDeleted(), 0))
                .map(ChartMapper::toDomain)
                .toList();

    }

    @Override
    public List<Chart> findRecentCharts(int limit) {
        QChartEntity chart = QChartEntity.chartEntity;

        return queryFactory.selectFrom(chart)
                .where(chart.isDeleted.eq(0))
                .orderBy(chart.createdAt.desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(ChartMapper::toDomain)
                .toList();
    }

}
