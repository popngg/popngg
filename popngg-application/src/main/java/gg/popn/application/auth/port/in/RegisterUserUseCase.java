package gg.popn.application.auth.port.in;

import gg.popn.application.auth.dto.command.RegisterCommand;
import gg.popn.application.auth.dto.response.LoginResult;

public interface RegisterUserUseCase {
    boolean exists(String poptomoId);
    LoginResult register(RegisterCommand command);
}
