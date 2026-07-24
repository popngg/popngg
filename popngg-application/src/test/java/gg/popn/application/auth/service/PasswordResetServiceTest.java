package gg.popn.application.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gg.popn.application.auth.dto.command.ConfirmPasswordResetCommand;
import gg.popn.application.auth.dto.command.RequestPasswordResetCommand;
import gg.popn.application.auth.exception.InvalidPasswordResetTokenException;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.PasswordResetAccountPort;
import gg.popn.application.auth.port.out.PasswordResetMailPort;
import gg.popn.application.auth.port.out.PasswordResetTokenPort;
import gg.popn.application.auth.port.out.ResetTokenPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-24T00:00:00Z");
    private static final String EMAIL = "account@example.invalid";

    @Mock
    private PasswordResetAccountPort accountPort;
    @Mock
    private PasswordResetTokenPort storedTokenPort;
    @Mock
    private ResetTokenPort resetTokenPort;
    @Mock
    private PasswordResetMailPort mailPort;
    @Mock
    private PasswordHasherPort passwordHasher;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                accountPort,
                storedTokenPort,
                resetTokenPort,
                mailPort,
                passwordHasher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                30);
    }

    @Test
    void requestHasTheSameObservableResultForAnUnknownAccount() {
        when(accountPort.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.request(new RequestPasswordResetCommand(EMAIL));

        verify(resetTokenPort, never()).generate();
        verify(storedTokenPort, never()).save(any(Long.class), any(), any(), any());
        verify(mailPort, never()).sendResetLink(any(), any());
    }

    @Test
    void storesOnlyTheHashAndInvalidatesOlderUnusedTokens() {
        when(accountPort.findByEmail(EMAIL))
                .thenReturn(Optional.of(new PasswordResetAccountPort.PasswordResetAccount(7L, EMAIL)));
        when(resetTokenPort.generate()).thenReturn("raw-value");
        when(resetTokenPort.hash("raw-value")).thenReturn("hashed-value");

        service.request(new RequestPasswordResetCommand(EMAIL));

        verify(storedTokenPort).invalidateUnusedForUser(7L, NOW);
        var hash = ArgumentCaptor.forClass(String.class);
        var expiry = ArgumentCaptor.forClass(Instant.class);
        verify(storedTokenPort).save(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(EMAIL),
                hash.capture(),
                expiry.capture());
        assertThat(hash.getValue()).isEqualTo("hashed-value");
        assertThat(expiry.getValue()).isEqualTo(NOW.plusSeconds(1_800));
        verify(mailPort).sendResetLink(EMAIL, "raw-value");
    }

    @Test
    void rejectsExpiredOrPreviouslyUsedTokens() {
        when(resetTokenPort.hash("presented")).thenReturn("hashed-value");
        when(storedTokenPort.findByHash("hashed-value"))
                .thenReturn(Optional.of(new PasswordResetTokenPort.StoredPasswordResetToken(
                        3L,
                        7L,
                        NOW,
                        null)));

        assertThatThrownBy(() -> service.confirm(
                new ConfirmPasswordResetCommand("presented", "new-value")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(accountPort, never()).updatePassword(any(Long.class), any());
    }

    @Test
    void consumesTheTokenBeforeUpdatingThePassword() {
        when(resetTokenPort.hash("presented")).thenReturn("hashed-value");
        when(storedTokenPort.findByHash("hashed-value"))
                .thenReturn(Optional.of(new PasswordResetTokenPort.StoredPasswordResetToken(
                        3L,
                        7L,
                        NOW.plusSeconds(60),
                        null)));
        when(storedTokenPort.markUsedIfAvailable(3L, NOW)).thenReturn(true);
        when(passwordHasher.hash("new-value")).thenReturn("new-hash");

        service.confirm(new ConfirmPasswordResetCommand("presented", "new-value"));

        verify(storedTokenPort).markUsedIfAvailable(3L, NOW);
        verify(accountPort).updatePassword(7L, "new-hash");
    }

    @Test
    void concurrentReuseFailsWhenTheAtomicConsumeLoses() {
        when(resetTokenPort.hash("presented")).thenReturn("hashed-value");
        when(storedTokenPort.findByHash("hashed-value"))
                .thenReturn(Optional.of(new PasswordResetTokenPort.StoredPasswordResetToken(
                        3L,
                        7L,
                        NOW.plusSeconds(60),
                        null)));
        when(storedTokenPort.markUsedIfAvailable(3L, NOW)).thenReturn(false);

        assertThatThrownBy(() -> service.confirm(
                new ConfirmPasswordResetCommand("presented", "new-value")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(accountPort, never()).updatePassword(any(Long.class), any());
    }
}
