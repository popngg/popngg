package gg.popn.application.account.service;

import gg.popn.application.account.port.out.AccountSettingsPort;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AdminPasswordResetServiceTest {
    private final AccountSettingsPort accounts = mock(AccountSettingsPort.class);
    private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);

    @Test
    void marksProductionConstructorForSpringInjection() {
        assertThat(Arrays.stream(AdminPasswordResetService.class.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == 2)
                .findFirst().orElseThrow().isAnnotationPresent(Autowired.class)).isTrue();
    }

    @Test
    void createsTemporaryPasswordAndStoresItsClientDigestAsBcrypt() throws Exception {
        when(accounts.passwordHash("1234-5678-9012")).thenReturn("old-hash");
        when(hasher.hash(anyString())).thenReturn("bcrypt");
        var service = new AdminPasswordResetService(accounts, hasher, new SecureRandom(new byte[]{1}));

        String temporaryPassword = service.reset("1234-5678-9012");

        assertThat(temporaryPassword).matches("[A-Za-z0-9]{6}");
        String digest = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(temporaryPassword.getBytes(StandardCharsets.UTF_8)));
        verify(hasher).hash(digest);
        verify(accounts).updatePasswordHash("1234-5678-9012", "bcrypt");
    }

    @Test
    void rejectsInvalidIdBeforeChangingPassword() {
        var service = new AdminPasswordResetService(accounts, hasher, new SecureRandom());
        assertThatThrownBy(() -> service.reset("invalid")).isInstanceOf(RuntimeException.class);
        verify(accounts, never()).updatePasswordHash(anyString(), anyString());
    }
}
