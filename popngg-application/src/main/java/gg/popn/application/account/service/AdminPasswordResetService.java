package gg.popn.application.account.service;

import gg.popn.application.account.port.in.AdminPasswordResetUseCase;
import gg.popn.application.account.port.out.AccountSettingsPort;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.domain.user.model.field.PoptomoId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPasswordResetService implements AdminPasswordResetUseCase {
    private static final char[] PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private final AccountSettingsPort accounts;
    private final PasswordHasherPort passwordHasher;
    private final SecureRandom random;

    public AdminPasswordResetService(AccountSettingsPort accounts, PasswordHasherPort passwordHasher) {
        this(accounts, passwordHasher, new SecureRandom());
    }

    AdminPasswordResetService(AccountSettingsPort accounts, PasswordHasherPort passwordHasher,
            SecureRandom random) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.random = random;
    }

    @Override
    @Transactional
    public String reset(String poptomoId) {
        String normalized = PoptomoId.of(poptomoId == null ? null : poptomoId.strip()).getValue();
        accounts.passwordHash(normalized);
        String temporaryPassword = temporaryPassword();
        accounts.updatePasswordHash(normalized, passwordHasher.hash(sha256(temporaryPassword)));
        return temporaryPassword;
    }

    private String temporaryPassword() {
        StringBuilder value = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            value.append(PASSWORD_CHARS[random.nextInt(PASSWORD_CHARS.length)]);
        }
        return value.toString();
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
