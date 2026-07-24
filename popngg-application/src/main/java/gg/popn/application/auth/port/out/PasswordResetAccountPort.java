package gg.popn.application.auth.port.out;

import java.util.Optional;

public interface PasswordResetAccountPort {
    Optional<PasswordResetAccount> findByEmail(String email);

    void updatePassword(long userId, String passwordHash);

    record PasswordResetAccount(long userId, String email) {
    }
}
