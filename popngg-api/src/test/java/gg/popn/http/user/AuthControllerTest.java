package gg.popn.http.user;

import gg.popn.application.auth.dto.response.LoginResult;
import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.port.in.PasswordResetUseCase;
import gg.popn.application.auth.port.in.RegisterUserUseCase;
import gg.popn.application.user.dto.query.UserProfileQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.port.in.UserProfileUseCase;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.http.user.request.LoginRequest;
import gg.popn.http.user.request.PasswordResetConfirmRequest;
import gg.popn.http.user.request.PasswordResetRequest;
import gg.popn.http.user.request.RegisterRequest;
import gg.popn.infra.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    private static final String ID = "1234-5678-9012";
    private static final String PASSWORD = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private final AuthenticateUserUseCase authenticate = mock(AuthenticateUserUseCase.class);
    private final PasswordResetUseCase passwordReset = mock(PasswordResetUseCase.class);
    private final RegisterUserUseCase register = mock(RegisterUserUseCase.class);
    private final UserProfileUseCase userProfile = mock(UserProfileUseCase.class);
    private final AuthController controller = new AuthController(
            authenticate, passwordReset, register, userProfile);

    @Test
    void loginReturnsNullDataAndSecureCookie() {
        when(authenticate.login(any())).thenReturn(loginResult());
        var servletResponse = new MockHttpServletResponse();

        var response = controller.login(new LoginRequest(ID, PASSWORD), servletResponse);

        assertThat(response.getData()).isNull();
        assertThat(servletResponse.getHeader("Set-Cookie"))
                .contains("access_token=token", "Max-Age=3600", "HttpOnly", "Secure");
    }

    @Test
    void registrationUsesStatusOnly() {
        when(register.exists(ID)).thenReturn(true, false);
        assertThat(controller.registration(ID).getStatusCode().value()).isEqualTo(200);
        assertThat(controller.registration(ID).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void registerMapsHiddenFlagAndReturnsCookie() {
        when(register.register(any())).thenReturn(loginResult());
        var servletResponse = new MockHttpServletResponse();

        var response = controller.register(new RegisterRequest(ID, PASSWORD, true), servletResponse);

        assertThat(response.getData().profile().poptomoId()).isEqualTo(ID);
        verify(register).register(argThat(command -> command.hidden() && command.poptomoId().equals(ID)));
        assertThat(servletResponse.getHeader("Set-Cookie")).contains("access_token=token");
    }

    @Test
    void delegatesPasswordResetAndReturnsAuthenticatedSession() {
        assertThat(controller.requestPasswordReset(new PasswordResetRequest("a@example.com")).getCode().getValue())
                .isEqualTo("SUCCESS");
        assertThat(controller.confirmPasswordReset(new PasswordResetConfirmRequest("reset", PASSWORD)).getCode().getValue())
                .isEqualTo("SUCCESS");
        verify(passwordReset).request(argThat(command -> command.email().equals("a@example.com")));
        verify(passwordReset).confirm(argThat(command -> command.token().equals("reset")));

        var principal = new CustomUserPrincipal(PoptomoId.of(ID), UserRole.of("USER"));
        when(userProfile.get(new UserProfileQuery(ID))).thenReturn(profile());
        var session = controller.session(principal).getData();
        assertThat(session.poptomoId()).isEqualTo(ID);
        assertThat(session.userName()).isEqualTo("name");
        assertThat(session.avatarUrl()).isNull();
    }

    @Test
    void anonymousSessionReturnsNullData() {
        assertThat(controller.session(null).getData()).isNull();
        verifyNoInteractions(userProfile);
    }

    @Test
    void logoutExpiresAccessTokenCookie() {
        var servletResponse = new MockHttpServletResponse();

        var response = controller.logout(servletResponse);

        assertThat(response.getData()).isNull();
        assertThat(servletResponse.getHeader("Set-Cookie"))
                .contains("access_token=", "Max-Age=0", "HttpOnly", "Secure", "Path=/");
    }

    private static LoginResult loginResult() {
        return new LoginResult("token", "Bearer", 3600, "USER",
                new LoginResult.UserProfileSummary(ID, "name"));
    }

    private static UserProfileResult profile() {
        return new UserProfileResult(
                ID, "name", "character", "comment", null, false,
                0, 0, 0, 0, 0, 0, 0);
    }
}
