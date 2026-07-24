package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByPoptomoId(String poptomoId);
    Optional<UserEntity> findByUserName(String userName);
}
