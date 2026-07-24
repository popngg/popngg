package gg.popn.infra.db.adapter;

import gg.popn.application.chart.port.out.ChartCommandPort;
import gg.popn.domain.chart.model.Chart;
import gg.popn.infra.db.entity.ChartEntity;
import gg.popn.infra.db.jpa.ChartJpaRepository;
import gg.popn.infra.db.mapper.ChartMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChartCommandJpaAdapter implements ChartCommandPort {

    private final ChartJpaRepository chartJpaRepository;
    @Override
    public void save(Chart chart) {
        ChartEntity chartEntity = ChartMapper.toEntity(chart);
        chartJpaRepository.save(chartEntity);
    }
}
