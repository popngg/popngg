package gg.popn.application.auth.service;

import gg.popn.application.auth.dto.command.ConfirmPasswordResetCommand;
import gg.popn.application.auth.dto.command.RequestPasswordResetCommand;
import gg.popn.application.auth.exception.InvalidPasswordResetTokenException;
import gg.popn.application.auth.port.in.PasswordResetUseCase;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.PasswordResetAccountPort;
import gg.popn.application.auth.port.out.PasswordResetMailPort;
import gg.popn.application.auth.port.out.PasswordResetTokenPort;
import gg.popn.application.auth.port.out.ResetTokenPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService implements PasswordResetUseCase {
    private final PasswordResetAccountPort accountPort;
    private final PasswordResetTokenPort storedTokenPort;
    private final ResetTokenPort resetTokenPort;
    private final PasswordResetMailPort mailPort;
    private final PasswordHasherPort passwordHasher;
    private final Clock clock;

    private final long ttlMinutes;

    public PasswordResetService(
            PasswordResetAccountPort accountPort,
            PasswordResetTokenPort storedTokenPort,
            ResetTokenPort resetTokenPort,
            PasswordResetMailPort mailPort,
            PasswordHasherPort passwordHasher,
            Clock clock,
            @Value("${security.password-reset.ttl-minutes:30}") long ttlMinutes) {
        this.accountPort = accountPort;
        this.storedTokenPort = storedTokenPort;
        this.resetTokenPort = resetTokenPort;
        this.mailPort = mailPort;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.ttlMinutes = ttlMinutes;
    }

    @Override
    @Transactional
    public void request(RequestPasswordResetCommand command) {
        accountPort.findByEmail(command.email()).ifPresent(account -> {
            Instant now = clock.instant();
            String rawToken = resetTokenPort.generate();
            storedTokenPort.invalidateUnusedForUser(account.userId(), now);
            storedTokenPort.save(
                    account.userId(),
                    account.email(),
                    resetTokenPort.hash(rawToken),
                    now.plus(Duration.ofMinutes(ttlMinutes)));
            mailPort.sendResetLink(account.email(), rawToken);
        });
    }

    @Override
    @Transactional
    public void confirm(ConfirmPasswordResetCommand command) {
        Instant now = clock.instant();
        var stored = storedTokenPort.findByHash(resetTokenPort.hash(command.token()))
                .filter(token -> token.isAvailableAt(now))
                .orElseThrow(InvalidPasswordResetTokenException::new);

        if (!storedTokenPort.markUsedIfAvailable(stored.tokenId(), now)) {
            throw new InvalidPasswordResetTokenException();
        }
        accountPort.updatePassword(
                stored.userId(),
                passwordHasher.hash(command.newPassword()));
    }
}
