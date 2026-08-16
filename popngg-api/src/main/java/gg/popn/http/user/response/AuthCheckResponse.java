package gg.popn.http.user.response;

import gg.popn.infra.security.CustomUserPrincipal;

public record AuthCheckResponse(String poptomoId, String role) {
    public static AuthCheckResponse from(CustomUserPrincipal principal) {
        return new AuthCheckResponse(
                principal.getPoptomoId().getValue(),
                principal.getRole().getValue());
    }
}
