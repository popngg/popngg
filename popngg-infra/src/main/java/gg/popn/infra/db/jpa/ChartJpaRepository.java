package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.ChartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChartJpaRepository extends JpaRepository<ChartEntity, Long> {
    List<ChartEntity> findAllBySongHashAndDifficulty(String songHash, Integer difficulty);
}
