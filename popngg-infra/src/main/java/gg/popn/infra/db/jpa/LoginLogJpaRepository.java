package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.LoginLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogJpaRepository extends JpaRepository<LoginLogEntity, Long> {
}
