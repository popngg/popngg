package gg.popn.application.auth.port.in;

import gg.popn.application.auth.port.in.command.LoginCommand;
import gg.popn.application.auth.port.in.result.AuthResult;


public interface  AuthenticateUserUseCase {

    AuthResult login(LoginCommand cmd);

    AuthResult loginWithoutHash(LoginCommand cmd);
}