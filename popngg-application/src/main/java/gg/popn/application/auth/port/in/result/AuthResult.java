package gg.popn.application.auth.port.in.result;

import gg.popn.domain.user.model.AuthPrincipal;

public record AuthResult(String accessToken, AuthPrincipal principal) {}