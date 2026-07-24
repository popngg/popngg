package gg.popn.infra.db.adapter;

import gg.popn.application.auth.port.out.PasswordResetAccountPort;
import gg.popn.infra.db.jpa.UserJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetAccountJpaAdapter implements PasswordResetAccountPort {
    private final UserJpaRepository repository;

    @Override
    public Optional<PasswordResetAccount> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(user -> new PasswordResetAccount(user.getId(), user.getEmail()));
    }

    @Override
    public void updatePassword(long userId, String passwordHash) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Password reset account is unavailable."));
        user.changePasswordHash(passwordHash);
        repository.save(user);
    }
}
