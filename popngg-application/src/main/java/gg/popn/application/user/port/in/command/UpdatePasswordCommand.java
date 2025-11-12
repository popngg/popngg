// src/main/java/gg/popn/application/user/port/in/command/UpdatePasswordCommand.java
package gg.popn.application.user.port.in.command;

import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;

public record UpdatePasswordCommand(PoptomoId poptomoId, Password password) {
}
