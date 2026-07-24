package gg.popn.infra.db.adapter;

import gg.popn.application.auth.port.out.PasswordResetTokenPort;
import gg.popn.infra.db.entity.PasswordResetTokenEntity;
import gg.popn.infra.db.jpa.PasswordResetTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenJpaAdapter implements PasswordResetTokenPort {
    private final PasswordResetTokenJpaRepository repository;

    @Override
    public void invalidateUnusedForUser(long userId, Instant usedAt) {
        repository.invalidateUnusedForUser(userId, usedAt);
    }

    @Override
    public void save(long userId, String email, String tokenHash, Instant expiresAt) {
        repository.save(PasswordResetTokenEntity.builder()
                .userId(userId)
                .email(email)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public Optional<StoredPasswordResetToken> findByHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash)
                .map(entity -> new StoredPasswordResetToken(
                        entity.getId(),
                        entity.getUserId(),
                        entity.getExpiresAt(),
                        entity.getUsedAt()));
    }

    @Override
    public boolean markUsedIfAvailable(long tokenId, Instant usedAt) {
        return repository.markUsedIfAvailable(tokenId, usedAt) == 1;
    }
}
