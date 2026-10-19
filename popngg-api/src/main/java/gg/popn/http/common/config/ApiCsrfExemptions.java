package gg.popn.http.common.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Browser writes use an exact configured Origin check; other clients must supply a CSRF token or Bearer token. */
final class ApiCsrfExemptions implements RequestMatcher {
    private final Set<String> origins;
    ApiCsrfExemptions(String configuredOrigins) {
        origins = Arrays.stream(configuredOrigins.split(",")).map(String::trim).map(origin -> {
            var uri = URI.create(origin);
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty()))
                throw new IllegalArgumentException("CORS origins must be explicit HTTP(S) origins without paths.");
            return origin;
        }).collect(Collectors.toUnmodifiableSet());
    }
    @Override public boolean matches(HttpServletRequest request) {
        // This endpoint authenticates the raw request using Discord's Ed25519 signature, never cookies.
        if ("POST".equals(request.getMethod()) && "/api/v1/discord/interactions".equals(request.getServletPath())) return true;
        // JwtAuthenticationFilter prefers this header over cookies and never falls back on invalid Bearer tokens.
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) return true;
        var headers = request.getHeaders("Origin");
        if (headers == null || !headers.hasMoreElements()) return false;
        String origin = headers.nextElement();
        return !headers.hasMoreElements() && origins.contains(origin);
    }
}
