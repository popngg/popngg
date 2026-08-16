package gg.popn.infra.security.adapter;

import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.infra.security.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {
    private static final String SECRET =
            "test-only-jwt-secret-with-at-least-sixty-four-characters-0123456789abcdef";

    @Test
    void issuesAndParsesTokenForConfiguredIssuer() {
        var provider = provider("popngg");
        var principal = AuthPrincipal.of(PoptomoId.of("0000-0000-0000"), UserRole.of("ADMIN"));

        var parsed = provider.parse(provider.issueAccessToken(principal).value());

        assertThat(parsed).isPresent();
        assertThat(parsed.orElseThrow().getPoptomoId().getValue()).isEqualTo("0000-0000-0000");
        assertThat(parsed.orElseThrow().getUserRole().getValue()).isEqualTo("ADMIN");
    }

    @Test
    void rejectsTokenIssuedForAnotherIssuer() {
        var token = provider("other-service").issueAccessToken(
                AuthPrincipal.of(PoptomoId.of("0000-0000-0000"), UserRole.of("USER"))).value();

        assertThat(provider("popngg").parse(token)).isEmpty();
    }

    private static JwtTokenProvider provider(String issuer) {
        var config = new JwtConfig();
        ReflectionTestUtils.setField(config, "secretKey", SECRET);
        ReflectionTestUtils.setField(config, "issuer", issuer);
        ReflectionTestUtils.setField(config, "expirationHours", 1L);
        return new JwtTokenProvider(config);
    }
}
