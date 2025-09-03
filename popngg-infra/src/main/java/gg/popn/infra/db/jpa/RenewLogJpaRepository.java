package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.RenewLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RenewLogJpaRepository extends JpaRepository<RenewLogEntity, Long> {
}
