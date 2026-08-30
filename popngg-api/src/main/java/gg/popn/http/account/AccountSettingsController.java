package gg.popn.http.account;

import gg.popn.application.account.dto.AccountSettings;
import gg.popn.application.account.dto.ProfileUpdate;
import gg.popn.application.account.exception.AccountSettingsException;
import gg.popn.application.account.port.in.AccountSettingsUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/account")
public class AccountSettingsController {
    private final AccountSettingsUseCase settings;

    public AccountSettingsController(AccountSettingsUseCase settings) {
        this.settings = settings;
    }

    @GetMapping("/settings")
    public SuccessResponse<SettingsResponse> get() {
        return success(SettingsResponse.from(settings.get()));
    }

    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<SettingsResponse> updateProfile(
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String isPrivate,
            @RequestPart(required = false) MultipartFile avatar,
            @RequestParam(required = false) String removeAvatar) throws java.io.IOException {
        if (comment == null || !("true".equals(isPrivate) || "false".equals(isPrivate))) {
            throw invalidProfile("comment and isPrivate are required.");
        }
        if (removeAvatar != null && !"true".equals(removeAvatar)) {
            throw invalidProfile("removeAvatar must be true when supplied.");
        }
        ProfileUpdate.Avatar uploaded = avatar == null ? null
                : new ProfileUpdate.Avatar(avatar.getBytes(), avatar.getContentType());
        return success(SettingsResponse.from(settings.updateProfile(new ProfileUpdate(
                comment, Boolean.parseBoolean(isPrivate), uploaded, "true".equals(removeAvatar)))));
    }

    @PatchMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SuccessResponse<Void> updatePassword(@RequestBody(required = false) PasswordRequest request) {
        if (request == null) {
            throw new AccountSettingsException(400, "INVALID_PASSWORD", "The password request is required.");
        }
        settings.changePassword(request.currentPassword(), request.newPassword());
        return success(null);
    }

    private static AccountSettingsException invalidProfile(String message) {
        return new AccountSettingsException(400, "INVALID_PROFILE", message);
    }

    private static <T> SuccessResponse<T> success(T data) {
        return SuccessResponse.<T>builder().code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS).data(data).build();
    }

    public record SettingsResponse(String avatarUrl, String comment, boolean isPrivate) {
        static SettingsResponse from(AccountSettings value) {
            return new SettingsResponse(value.avatarUrl(), value.comment(), value.privateProfile());
        }
    }

    public record PasswordRequest(String currentPassword, String newPassword) {
    }
}
