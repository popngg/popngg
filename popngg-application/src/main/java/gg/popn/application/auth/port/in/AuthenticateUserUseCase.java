package gg.popn.application.auth.port.in;

import gg.popn.application.auth.dto.command.LoginCommand;
import gg.popn.application.auth.dto.response.LoginResult;


public interface  AuthenticateUserUseCase {

    LoginResult login(LoginCommand cmd);
}
