package gg.popn.application.auth.port.in;

import gg.popn.application.auth.model.AuthPrincipal;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.Username;

public interface  AuthenticateUserUseCase {
    AuthResult login(Username username, Password password);

    record AuthResult(String accessToken, AuthPrincipal principal) {}
}