package gg.popn.application.auth.service;

import gg.popn.application.auth.dto.command.LoginCommand;
import gg.popn.application.auth.dto.command.RegisterCommand;
import gg.popn.application.auth.dto.response.LoginResult;
import gg.popn.application.auth.exception.AlreadyRegisteredException;
import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.port.in.RegisterUserUseCase;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.RegisterUserPort;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {
    private final RegisterUserPort users;
    private final PasswordHasherPort passwordHasher;
    private final AuthenticateUserUseCase authenticate;

    @Override
    public boolean exists(String poptomoId) {
        return users.exists(PoptomoId.of(poptomoId).getValue());
    }

    @Override
    public LoginResult register(RegisterCommand command) {
        var poptomoId = PoptomoId.of(command.poptomoId());
        var password = Password.of(command.password());
        if (users.exists(poptomoId.getValue())) {
            throw new AlreadyRegisteredException();
        }
        users.create(poptomoId.getValue(), passwordHasher.hash(password.getValue()), command.hidden());
        return authenticate.login(new LoginCommand(poptomoId, password));
    }
}
