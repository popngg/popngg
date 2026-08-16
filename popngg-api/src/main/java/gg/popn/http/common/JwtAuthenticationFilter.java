package gg.popn.http.common;


import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.infra.security.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenPort tokenPort; // 🔹 포트만 의존 (jjwt 모름)

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        TokenCandidate candidate = resolveToken(request);
        if (candidate == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AuthPrincipal> principalOpt = tokenPort.parse(candidate.value());

        if (principalOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthPrincipal authPrincipal = principalOpt.get();

        CustomUserPrincipal userDetails =
                new CustomUserPrincipal(authPrincipal.getPoptomoId(), authPrincipal.getUserRole());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (candidate.cookie()) {
            var renewed = tokenPort.issueAccessToken(authPrincipal);
            response.addHeader("Set-Cookie", org.springframework.http.ResponseCookie
                    .from("access_token", renewed.value()).httpOnly(true).secure(true)
                    .sameSite("Lax").path("/").maxAge(renewed.expiresInSeconds())
                    .build().toString());
        }

        filterChain.doFilter(request, response);
    }

    private static TokenCandidate resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) return new TokenCandidate(authHeader.substring(7), false);
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(cookie -> new TokenCandidate(cookie.getValue(), true)).findFirst().orElse(null);
    }

    private record TokenCandidate(String value, boolean cookie) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 토큰 없이 허용할 경로들
        return path.startsWith("/auth")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
