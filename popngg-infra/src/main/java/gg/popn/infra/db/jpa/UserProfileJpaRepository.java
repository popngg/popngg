package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, Long> {
}
