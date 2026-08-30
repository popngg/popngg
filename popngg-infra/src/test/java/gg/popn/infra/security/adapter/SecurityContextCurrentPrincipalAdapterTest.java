package gg.popn.infra.security.adapter;

import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.infra.security.CustomUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCurrentPrincipalAdapterTest {
    private final SecurityContextCurrentPrincipalAdapter adapter =
            new SecurityContextCurrentPrincipalAdapter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthPrincipalForAuthenticatedCustomUserPrincipal() {
        var principal = new CustomUserPrincipal(
                PoptomoId.of("1234-5678-9012"), UserRole.of("USER"));
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var currentPrincipal = adapter.get();

        assertThat(currentPrincipal).isPresent();
        assertThat(currentPrincipal.orElseThrow().getPoptomoId().getValue())
                .isEqualTo("1234-5678-9012");
        assertThat(currentPrincipal.orElseThrow().getUserRole().getValue()).isEqualTo("USER");
    }

    @Test
    void returnsEmptyWhenUnauthenticated() {
        assertThat(adapter.get()).isEmpty();
    }

    @Test
    void returnsExistingAuthPrincipal() {
        var principal = AuthPrincipal.of(
                PoptomoId.of("1234-5678-9012"), UserRole.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));

        assertThat(adapter.get()).contains(principal);
    }

    @Test
    void returnsEmptyForUnknownPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null));

        assertThat(adapter.get()).isEmpty();
    }
}
