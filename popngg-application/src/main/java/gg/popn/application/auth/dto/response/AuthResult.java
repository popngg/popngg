package gg.popn.application.auth.dto.response;

import gg.popn.domain.user.model.AuthPrincipal;

public record  AuthResult(String accessToken, AuthPrincipal principal) {}