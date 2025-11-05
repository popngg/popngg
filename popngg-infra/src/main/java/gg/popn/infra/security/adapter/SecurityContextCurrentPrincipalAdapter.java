package gg.popn.infra.security.adapter;

import gg.popn.application.auth.model.AuthPrincipal;
import gg.popn.application.auth.port.out.CurrentPrincipalPort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextCurrentPrincipalAdapter implements CurrentPrincipalPort {
    @Override
    public Optional<AuthPrincipal> get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) return Optional.empty();
        return Optional.of(p);
    }
}