package gg.popn.application.account.port.in;

import gg.popn.application.account.dto.AccountSettings;
import gg.popn.application.account.dto.ProfileUpdate;

public interface AccountSettingsUseCase {
    AccountSettings get();
    AccountSettings updateProfile(ProfileUpdate update);
    void changePassword(String currentPassword, String newPasswordDigest);
}
