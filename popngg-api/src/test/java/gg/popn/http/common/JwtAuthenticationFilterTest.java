package gg.popn.http.common;

import gg.popn.application.auth.port.out.IssuedAccessToken;
import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final TokenPort tokens = mock(TokenPort.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens);
    private final AuthPrincipal principal = AuthPrincipal.of(
            PoptomoId.of("1234-5678-9012"), UserRole.of("USER"));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesRequestWithoutToken() throws Exception {
        var chain = new MockFilterChain();
        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(tokens);
    }

    @Test
    void authenticatesBearerTokenWithoutRenewingCookie() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer original");
        var response = new MockHttpServletResponse();
        when(tokens.parse("original")).thenReturn(Optional.of(principal));

        filter.doFilterInternal(request, response, new MockFilterChain());

        var authenticated = (gg.popn.infra.security.CustomUserPrincipal)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(authenticated.getPoptomoId().getValue()).isEqualTo("1234-5678-9012");
        assertThat(response.getHeader("Set-Cookie")).isNull();
        verify(tokens, never()).issueAccessToken(any());
    }

    @Test
    void authenticatesCookieAndSlidesExpiration() throws Exception {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("ignored", "x"), new Cookie("access_token", "cookie-token"));
        var response = new MockHttpServletResponse();
        when(tokens.parse("cookie-token")).thenReturn(Optional.of(principal));
        when(tokens.issueAccessToken(principal)).thenReturn(new IssuedAccessToken("renewed", 120));

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader("Set-Cookie"))
                .contains("access_token=renewed", "Max-Age=120", "HttpOnly", "Secure", "SameSite=None");
    }

    @Test
    void ignoresInvalidToken() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        when(tokens.parse("invalid")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void bypassesOnlyPublicAuthAndDocumentationPaths() {
        assertThat(filter.shouldNotFilter(request("/auth/login"))).isTrue();
        assertThat(filter.shouldNotFilter(request("/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(request("/v3/api-docs"))).isTrue();
        assertThat(filter.shouldNotFilter(request("/api/v1/auth/logout"))).isTrue();
        assertThat(filter.shouldNotFilter(request("/api/v1/auth/session"))).isFalse();
        assertThat(filter.shouldNotFilter(request("/api/v1/renewals"))).isFalse();
    }

    private static MockHttpServletRequest request(String path) {
        var request = new MockHttpServletRequest();
        request.setServletPath(path);
        return request;
    }
}
