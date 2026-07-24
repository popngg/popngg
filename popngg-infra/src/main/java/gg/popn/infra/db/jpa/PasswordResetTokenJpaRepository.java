package gg.popn.infra.db.jpa;

import gg.popn.infra.db.entity.PasswordResetTokenEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenJpaRepository
        extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update PasswordResetTokenEntity token
               set token.usedAt = :usedAt
             where token.userId = :userId
               and token.usedAt is null
            """)
    int invalidateUnusedForUser(
            @Param("userId") long userId,
            @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("""
            update PasswordResetTokenEntity token
               set token.usedAt = :usedAt
             where token.id = :tokenId
               and token.usedAt is null
               and token.expiresAt > :usedAt
            """)
    int markUsedIfAvailable(
            @Param("tokenId") long tokenId,
            @Param("usedAt") Instant usedAt);
}
