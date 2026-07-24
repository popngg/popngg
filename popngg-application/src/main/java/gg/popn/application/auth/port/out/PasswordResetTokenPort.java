package gg.popn.application.auth.port.out;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenPort {
    void invalidateUnusedForUser(long userId, Instant usedAt);

    void save(long userId, String email, String tokenHash, Instant expiresAt);

    Optional<StoredPasswordResetToken> findByHash(String tokenHash);

    boolean markUsedIfAvailable(long tokenId, Instant usedAt);

    record StoredPasswordResetToken(
            long tokenId,
            long userId,
            Instant expiresAt,
            Instant usedAt
    ) {
        public boolean isAvailableAt(Instant now) {
            return usedAt == null && expiresAt.isAfter(now);
        }
    }
}
