package gg.popn.application.auth.service;

import gg.popn.application.auth.port.out.CurrentPrincipalPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessManager {
    private final CurrentPrincipalPort current;

    public AuthPrincipal requireAuthenticated() {
        return current.get().orElseThrow(() -> new AccessDeniedException("UNAUTHENTICATED"));
    }

    public void requireRole(UserRole requiredRole) {
        var p = requireAuthenticated();
        if (!p.getUserRole().equals(requiredRole)) throw new AccessDeniedException("FORBIDDEN");
    }

    /** 자신(PoptomoId 동일) 또는 ADMIN만 허용 */
    public void requireSelfOrAdmin(PoptomoId owner) {
        var p = requireAuthenticated();
        boolean isSelf = p.getPoptomoId().equals(owner);
        boolean isAdmin = p.getUserRole().equals(UserRole.from("ADMIN"));
        if (!(isSelf || isAdmin)) throw new AccessDeniedException("FORBIDDEN");
    }
}
