package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.PlaydataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaydataJpaRepository extends JpaRepository<PlaydataEntity,Long> {
}
