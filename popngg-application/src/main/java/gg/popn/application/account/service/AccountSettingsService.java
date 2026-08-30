package gg.popn.application.account.service;

import gg.popn.application.account.dto.AccountSettings;
import gg.popn.application.account.dto.ProfileUpdate;
import gg.popn.application.account.exception.AccountSettingsException;
import gg.popn.application.account.port.in.AccountSettingsUseCase;
import gg.popn.application.account.port.out.AccountSettingsPort;
import gg.popn.application.account.port.out.AvatarStoragePort;
import gg.popn.application.account.port.out.AvatarProcessingException;
import gg.popn.application.auth.port.out.CurrentPrincipalPort;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.PasswordVerificationPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSettingsService implements AccountSettingsUseCase {
    private static final int MAX_AVATAR_BYTES = 2 * 1024 * 1024;
    private final CurrentPrincipalPort principal;
    private final AccountSettingsPort accounts;
    private final AvatarStoragePort avatars;
    private final PasswordVerificationPort passwordVerification;
    private final PasswordHasherPort passwordHasher;

    public AccountSettingsService(CurrentPrincipalPort principal, AccountSettingsPort accounts,
            AvatarStoragePort avatars, PasswordVerificationPort passwordVerification,
            PasswordHasherPort passwordHasher) {
        this.principal = principal;
        this.accounts = accounts;
        this.avatars = avatars;
        this.passwordVerification = passwordVerification;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public AccountSettings get() {
        return accounts.find(poptomoId());
    }

    @Override
    @Transactional
    public AccountSettings updateProfile(ProfileUpdate update) {
        if (update.comment() == null) fail(400, "INVALID_PROFILE", "comment is required.");
        if (update.comment().codePointCount(0, update.comment().length()) > 50)
            fail(400, "INVALID_COMMENT", "Comment must be 50 characters or fewer.");
        if (update.avatar() != null && update.removeAvatar())
            fail(400, "INVALID_PROFILE", "avatar and removeAvatar cannot be used together.");
        if (update.avatar() != null) {
            validateAvatar(update.avatar());
        }

        String id = poptomoId();
        AccountSettings previous = accounts.find(id);
        String avatarUrl = previous.avatarUrl();
        boolean avatarChanged = false;
        if (update.avatar() != null) {
            try {
                avatarUrl = avatars.upload(id, update.avatar().bytes(), update.avatar().contentType());
            } catch (AvatarProcessingException exception) {
                fail(400, "INVALID_AVATAR_TYPE", "Avatar image could not be decoded.");
            }
            avatarChanged = true;
        } else if (update.removeAvatar()) {
            avatarUrl = null;
            avatarChanged = true;
        }
        AccountSettings result = accounts.updateProfile(id, update.comment(), update.privateProfile(),
                avatarUrl, avatarChanged);
        if (avatarChanged && previous.avatarUrl() != null && !previous.avatarUrl().equals(avatarUrl)) {
            avatars.deleteIfManaged(previous.avatarUrl());
        }
        return result;
    }

    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPasswordDigest) {
        if (currentPassword == null || !currentPassword.matches("[A-Za-z0-9]{4,16}")
                || newPasswordDigest == null || !newPasswordDigest.matches("[0-9a-f]{64}")) {
            fail(400, "INVALID_PASSWORD", "The password request format is invalid.");
        }
        String id = poptomoId();
        if (!passwordVerification.matches(sha256(currentPassword), accounts.passwordHash(id))) {
            fail(401, "INVALID_PASSWORD", "The current password is incorrect.");
        }
        accounts.updatePasswordHash(id, passwordHasher.hash(newPasswordDigest));
    }

    private void validateAvatar(ProfileUpdate.Avatar avatar) {
        if (avatar.bytes() == null || avatar.bytes().length == 0)
            fail(400, "INVALID_AVATAR_TYPE", "Avatar file is empty.");
        if (avatar.bytes().length > MAX_AVATAR_BYTES)
            fail(400, "AVATAR_TOO_LARGE", "Avatar must not exceed 2 MiB.");
        String detected = detectType(avatar.bytes());
        if (detected == null || !detected.equals(avatar.contentType()))
            fail(400, "INVALID_AVATAR_TYPE", "Avatar must be JPEG, PNG, or WebP.");
    }

    private static String detectType(byte[] b) {
        if (b.length >= 8 && (b[0]&255)==0x89 && b[1]=='P' && b[2]=='N' && b[3]=='G') return "image/png";
        if (b.length >= 3 && (b[0]&255)==0xff && (b[1]&255)==0xd8 && (b[2]&255)==0xff) return "image/jpeg";
        if (b.length >= 12 && b[0]=='R' && b[1]=='I' && b[2]=='F' && b[3]=='F'
                && b[8]=='W' && b[9]=='E' && b[10]=='B' && b[11]=='P') return "image/webp";
        return null;
    }

    private String poptomoId() {
        return principal.get().orElseThrow(() -> new AccountSettingsException(401, "UNAUTHENTICATED",
                "Login session is missing or expired.")).getPoptomoId().getValue();
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void fail(int status, String code, String message) {
        throw new AccountSettingsException(status, code, message);
    }
}
