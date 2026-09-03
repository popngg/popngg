package gg.popn.infra.db.adapter;

import gg.popn.application.account.dto.AccountSettings;
import gg.popn.application.account.exception.AccountSettingsException;
import gg.popn.application.account.port.out.AccountSettingsPort;
import gg.popn.infra.db.jpa.UserJpaRepository;
import gg.popn.infra.db.jpa.UserProfileJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountSettingsJpaAdapter implements AccountSettingsPort {
    private final UserJpaRepository users;
    private final UserProfileJpaRepository profiles;
    private final JdbcTemplate jdbc;

    public AccountSettingsJpaAdapter(UserJpaRepository users, UserProfileJpaRepository profiles,
            JdbcTemplate jdbc) {
        this.users = users;
        this.profiles = profiles;
        this.jdbc = jdbc;
    }

    @Override
    public AccountSettings find(String poptomoId) {
        var profile = user(poptomoId).getProfile();
        return new AccountSettings(profile.getProfileImageUrl(), profile.getComment(), profile.isHidden());
    }

    @Override
    @Transactional
    public AccountSettings updateProfile(String poptomoId, String comment, boolean privateProfile,
            String avatarUrl, boolean avatarChanged) {
        UserDirectoryState.invalidate(jdbc);
        var user = user(poptomoId);
        user.getProfile().updateSettings(comment, privateProfile, avatarUrl, avatarChanged, LocalDateTime.now());
        profiles.save(user.getProfile());
        return new AccountSettings(user.getProfile().getProfileImageUrl(), user.getProfile().getComment(),
                user.getProfile().isHidden());
    }

    @Override
    public String passwordHash(String poptomoId) {
        return user(poptomoId).getPasswordHash();
    }

    @Override
    public void updatePasswordHash(String poptomoId, String passwordHash) {
        var user = user(poptomoId);
        user.changePasswordHash(passwordHash);
        users.save(user);
    }

    private gg.popn.infra.db.entity.UserEntity user(String poptomoId) {
        return users.findByPoptomoId(poptomoId).orElseThrow(() ->
                new AccountSettingsException(401, "UNAUTHENTICATED", "Login session is missing or expired."));
    }
}
