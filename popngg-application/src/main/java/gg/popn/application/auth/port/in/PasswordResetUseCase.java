package gg.popn.application.auth.port.in;

import gg.popn.application.auth.dto.command.ConfirmPasswordResetCommand;
import gg.popn.application.auth.dto.command.RequestPasswordResetCommand;

public interface PasswordResetUseCase {
    void request(RequestPasswordResetCommand command);

    void confirm(ConfirmPasswordResetCommand command);
}
