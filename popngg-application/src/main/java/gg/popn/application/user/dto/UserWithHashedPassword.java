package gg.popn.application.user.dto;

import gg.popn.domain.user.model.User;

public record UserWithHashedPassword(User user, String passwordHash) {}