package gg.popn.application.auth.service;

import gg.popn.application.auth.dto.command.RegisterCommand;
import gg.popn.application.auth.dto.response.LoginResult;
import gg.popn.application.auth.exception.AlreadyRegisteredException;
import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.RegisterUserPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterUserServiceTest {
    private static final String ID = "1234-5678-9012";
    private static final String PASSWORD = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private final RegisterUserPort users = mock(RegisterUserPort.class);
    private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);
    private final AuthenticateUserUseCase authenticate = mock(AuthenticateUserUseCase.class);
    private final RegisterUserService service = new RegisterUserService(users, hasher, authenticate);

    @Test
    void createsHiddenAccountAndReturnsLoginResult() {
        var expected = new LoginResult("token", "Bearer", 60, "USER",
                new LoginResult.UserProfileSummary(ID, ID));
        when(hasher.hash(PASSWORD)).thenReturn("hash");
        when(authenticate.login(any())).thenReturn(expected);

        var result = service.register(new RegisterCommand(ID, PASSWORD, true));

        assertThat(result).isSameAs(expected);
        verify(users).create(ID, "hash", true);
    }

    @Test
    void rejectsExistingAccountBeforeHashing() {
        when(users.exists(ID)).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterCommand(ID, PASSWORD, false)))
                .isInstanceOf(AlreadyRegisteredException.class);
        verifyNoInteractions(hasher, authenticate);
    }
}
