package gg.popn.http.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gg.popn.application.account.dto.AccountSettings;
import gg.popn.application.account.dto.ProfileUpdate;
import gg.popn.application.account.exception.AccountSettingsException;
import gg.popn.application.account.port.in.AccountSettingsUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class AccountSettingsControllerTest {
    private final AccountSettingsUseCase useCase = mock(AccountSettingsUseCase.class);
    private final AccountSettingsController controller = new AccountSettingsController(useCase);

    @Test
    void getsSettingsInFrontendShape() {
        when(useCase.get()).thenReturn(new AccountSettings("avatar", "comment", true));

        var response = controller.get();

        assertThat(response.getData().avatarUrl()).isEqualTo("avatar");
        assertThat(response.getData().comment()).isEqualTo("comment");
        assertThat(response.getData().isPrivate()).isTrue();
    }

    @Test
    void passesMultipartProfileFieldsToUseCase() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        var file = new MockMultipartFile("avatar", "avatar.png", "image/png", png);
        when(useCase.updateProfile(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AccountSettings("new", "hello", false));

        var response = controller.updateProfile("hello", "false", file, null);

        var command = ArgumentCaptor.forClass(ProfileUpdate.class);
        verify(useCase).updateProfile(command.capture());
        assertThat(command.getValue().comment()).isEqualTo("hello");
        assertThat(command.getValue().privateProfile()).isFalse();
        assertThat(command.getValue().avatar().bytes()).isEqualTo(png);
        assertThat(response.getData().avatarUrl()).isEqualTo("new");
    }

    @Test
    void supportsAvatarRemovalWithoutUpload() throws Exception {
        when(useCase.updateProfile(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AccountSettings(null, "hello", true));

        controller.updateProfile("hello", "true", null, "true");

        verify(useCase).updateProfile(new ProfileUpdate("hello", true, null, true));
    }

    @Test
    void rejectsMissingAndMalformedMultipartFields() {
        assertInvalidProfile(() -> controller.updateProfile(null, "true", null, null));
        assertInvalidProfile(() -> controller.updateProfile("hello", null, null, null));
        assertInvalidProfile(() -> controller.updateProfile("hello", "yes", null, null));
        assertInvalidProfile(() -> controller.updateProfile("hello", "true", null, "false"));
    }

    @Test
    void changesPasswordAndReturnsNullData() {
        var response = controller.updatePassword(
                new AccountSettingsController.PasswordRequest("Abcd1", "a".repeat(64)));

        verify(useCase).changePassword("Abcd1", "a".repeat(64));
        assertThat(response.getData()).isNull();
    }

    @Test
    void rejectsMissingPasswordBody() {
        assertThatThrownBy(() -> controller.updatePassword(null))
                .isInstanceOfSatisfying(AccountSettingsException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("INVALID_PASSWORD");
                    assertThat(exception.status()).isEqualTo(400);
                });
    }

    private static void assertInvalidProfile(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AccountSettingsException.class,
                exception -> assertThat(exception.code()).isEqualTo("INVALID_PROFILE"));
    }
}
