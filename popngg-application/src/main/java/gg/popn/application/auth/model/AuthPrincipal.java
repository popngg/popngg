package gg.popn.application.auth.model;

import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;

public record AuthPrincipal(PoptomoId poptomoId, UserRole role) {}