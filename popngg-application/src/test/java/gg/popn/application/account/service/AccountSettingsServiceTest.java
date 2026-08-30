package gg.popn.application.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gg.popn.application.account.dto.AccountSettings;
import gg.popn.application.account.dto.ProfileUpdate;
import gg.popn.application.account.exception.AccountSettingsException;
import gg.popn.application.account.port.out.AccountSettingsPort;
import gg.popn.application.account.port.out.AvatarStoragePort;
import gg.popn.application.auth.port.out.CurrentPrincipalPort;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.PasswordVerificationPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountSettingsServiceTest {
    private final CurrentPrincipalPort principal = mock(CurrentPrincipalPort.class);
    private final AccountSettingsPort accounts = mock(AccountSettingsPort.class);
    private final AvatarStoragePort avatars = mock(AvatarStoragePort.class);
    private final PasswordVerificationPort verifier = mock(PasswordVerificationPort.class);
    private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);
    private final AccountSettingsService service = new AccountSettingsService(
            principal, accounts, avatars, verifier, hasher);

    @BeforeEach
    void authenticated() {
        when(principal.get()).thenReturn(Optional.of(AuthPrincipal.of(
                PoptomoId.of("1234-5678-9012"), UserRole.of("USER"))));
    }

    @Test
    void returnsCurrentSettings() {
        var expected = new AccountSettings("https://static/avatar.png", "hello", true);
        when(accounts.find("1234-5678-9012")).thenReturn(expected);
        assertThat(service.get()).isEqualTo(expected);
    }

    @Test
    void uploadsValidatedPngAndDeletesPreviousManagedAvatar() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        var before = new AccountSettings("https://static/old.png", "old", false);
        var after = new AccountSettings("https://static/new.png", "new", true);
        when(accounts.find(anyString())).thenReturn(before);
        when(avatars.upload(anyString(), org.mockito.ArgumentMatchers.any(), anyString()))
                .thenReturn(after.avatarUrl());
        when(accounts.updateProfile(anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean(),
                anyString(), org.mockito.ArgumentMatchers.eq(true))).thenReturn(after);

        assertThat(service.updateProfile(new ProfileUpdate("new", true,
                new ProfileUpdate.Avatar(png, "image/png"), false))).isEqualTo(after);
        verify(avatars).deleteIfManaged(before.avatarUrl());
    }

    @Test
    void rejectsSpoofedMimeTypeAndOversizedAvatar() {
        assertCode(() -> service.updateProfile(new ProfileUpdate("ok", false,
                new ProfileUpdate.Avatar(new byte[]{1, 2, 3}, "image/png"), false)),
                "INVALID_AVATAR_TYPE");
        assertCode(() -> service.updateProfile(new ProfileUpdate("ok", false,
                new ProfileUpdate.Avatar(new byte[2 * 1024 * 1024 + 1], "image/png"), false)),
                "AVATAR_TOO_LARGE");
        verify(avatars, never()).upload(anyString(), org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void validatesProfileContract() {
        assertCode(() -> service.updateProfile(new ProfileUpdate("x".repeat(51), false, null, false)),
                "INVALID_COMMENT");
        assertCode(() -> service.updateProfile(new ProfileUpdate("ok", false,
                new ProfileUpdate.Avatar(new byte[8], "image/png"), true)), "INVALID_PROFILE");
    }

    @Test
    void hashesCurrentPlaintextBeforeVerificationAndStoresBcryptOfNewDigest() throws Exception {
        String digest = "a".repeat(64);
        String currentDigest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest("Abcd1".getBytes(StandardCharsets.UTF_8)));
        when(accounts.passwordHash(anyString())).thenReturn("stored");
        when(verifier.matches(currentDigest, "stored")).thenReturn(true);
        when(hasher.hash(digest)).thenReturn("bcrypt");

        service.changePassword("Abcd1", digest);

        verify(accounts).updatePasswordHash("1234-5678-9012", "bcrypt");
    }

    @Test
    void rejectsMalformedOrIncorrectPasswordsWithSpecifiedStatus() {
        assertCode(() -> service.changePassword("bad!", "a".repeat(64)), "INVALID_PASSWORD");
        when(accounts.passwordHash(anyString())).thenReturn("stored");
        when(verifier.matches(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword("Abcd1", "a".repeat(64)))
                .isInstanceOfSatisfying(AccountSettingsException.class, e -> {
                    assertThat(e.code()).isEqualTo("INVALID_PASSWORD");
                    assertThat(e.status()).isEqualTo(401);
                });
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, String code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(AccountSettingsException.class,
                e -> assertThat(e.code()).isEqualTo(code));
    }
}
