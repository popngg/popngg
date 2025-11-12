package gg.popn.application.user.port.out.data;

import gg.popn.domain.user.model.User;

public record UserWithHashedPassword(User user, String passwordHash) {}