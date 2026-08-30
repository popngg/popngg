package gg.popn.infra.security.adapter;

import gg.popn.application.auth.port.out.CurrentPrincipalPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.infra.security.CustomUserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextCurrentPrincipalAdapter implements CurrentPrincipalPort {
    @Override
    public Optional<AuthPrincipal> get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();

        if (auth.getPrincipal() instanceof CustomUserPrincipal p) {
            return Optional.of(p.toAuthPrincipal());
        }
        if (auth.getPrincipal() instanceof AuthPrincipal p) return Optional.of(p);

        return Optional.empty();
    }
}
