package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    @EntityGraph(attributePaths = "profile")
    Optional<UserEntity> findByPoptomoId(String poptomoId);

    Optional<UserEntity> findByEmail(String email);
    @Query("select u from UserEntity u join fetch u.profile where u.profile.userName = :userName")
    Optional<UserEntity> findByUserName(@Param("userName") String userName);
}
