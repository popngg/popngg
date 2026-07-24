package gg.popn.application.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gg.popn.application.auth.dto.command.LoginCommand;
import gg.popn.application.auth.port.out.IssuedAccessToken;
import gg.popn.application.auth.port.out.LoginAuditPort;
import gg.popn.application.auth.port.out.PasswordVerificationPort;
import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.application.user.dto.UserWithHashedPassword;
import gg.popn.application.user.port.out.LoadUserPort;
import gg.popn.domain.user.model.User;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.domain.user.model.field.Username;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {
    private static final PoptomoId POPTOMO_ID = PoptomoId.of("1234-5678-9012");
    private static final Password PASSWORD = Password.of(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private PasswordVerificationPort passwordVerifier;
    @Mock
    private TokenPort tokenPort;
    @Mock
    private LoginAuditPort loginAudit;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(
                loadUserPort,
                passwordVerifier,
                tokenPort,
                loginAudit);
    }

    @Test
    void returnsAccessTokenRoleAndMinimalProfileForValidCredentials() {
        var user = User.builder()
                .poptomoId(POPTOMO_ID)
                .username(Username.of("ポップ"))
                .role(UserRole.of("USER"))
                .build();
        when(loadUserPort.loadByPoptomoId(POPTOMO_ID))
                .thenReturn(Optional.of(new UserWithHashedPassword(user, "stored")));
        when(passwordVerifier.matches(PASSWORD.getValue(), "stored")).thenReturn(true);
        when(tokenPort.issueAccessToken(userPrincipal()))
                .thenReturn(new IssuedAccessToken("access-token", 43_200));

        var result = service.login(new LoginCommand(POPTOMO_ID, PASSWORD));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresInSeconds()).isEqualTo(43_200);
        assertThat(result.role()).isEqualTo("USER");
        assertThat(result.profile().poptomoId()).isEqualTo("1234-5678-9012");
        assertThat(result.profile().userName()).isEqualTo("ポップ");
        verify(loginAudit).recordSuccess(POPTOMO_ID);
    }

    @Test
    void recordsAGenericFailureWithoutIssuingAToken() {
        when(loadUserPort.loadByPoptomoId(POPTOMO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand(POPTOMO_ID, PASSWORD)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(loginAudit).recordInvalidCredentials(POPTOMO_ID);
        verify(tokenPort, never()).issueAccessToken(userPrincipal());
    }

    private static gg.popn.domain.user.model.AuthPrincipal userPrincipal() {
        return gg.popn.domain.user.model.AuthPrincipal.of(POPTOMO_ID, UserRole.of("USER"));
    }
}
