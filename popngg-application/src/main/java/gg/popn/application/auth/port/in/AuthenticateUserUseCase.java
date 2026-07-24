package gg.popn.application.auth.port.in;

import gg.popn.application.auth.dto.command.LoginCommand;
import gg.popn.application.auth.dto.response.AuthResult;


public interface  AuthenticateUserUseCase {

    AuthResult login(LoginCommand cmd);

    AuthResult loginWithoutHash(LoginCommand cmd);
}