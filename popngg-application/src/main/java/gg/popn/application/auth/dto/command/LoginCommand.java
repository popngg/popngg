package gg.popn.application.auth.dto.command;

import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;

public record LoginCommand(PoptomoId poptomoId, Password password) {}
