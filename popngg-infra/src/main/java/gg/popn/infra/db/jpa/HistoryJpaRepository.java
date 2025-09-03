package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.HistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryJpaRepository extends JpaRepository<HistoryEntity, Long> {
}
