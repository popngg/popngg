package gg.popn.application.account.port.out;

import gg.popn.application.account.dto.AccountSettings;

public interface AccountSettingsPort {
    AccountSettings find(String poptomoId);
    AccountSettings updateProfile(String poptomoId, String comment, boolean privateProfile,
                                  String avatarUrl, boolean avatarChanged);
    String passwordHash(String poptomoId);
    void updatePasswordHash(String poptomoId, String passwordHash);
}
