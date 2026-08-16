package gg.popn.infra.security.adapter;

import gg.popn.application.auth.port.out.IssuedAccessToken;
import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.infra.security.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider implements TokenPort {
    private final JwtConfig jwtConfig;
    private final Key signingKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IssuedAccessToken issueAccessToken(AuthPrincipal principal) {
        Duration ttl = Duration.ofHours(jwtConfig.getExpirationHours());
        String value = Jwts.builder()
                .setSubject(principal.getPoptomoId().getValue())
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .claim("role", principal.getUserRole().getValue())
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
        return new IssuedAccessToken(value, ttl.toSeconds());
    }

    @Override
    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims claims = parseClaims(token);
            return Optional.of(AuthPrincipal.of(
                    PoptomoId.of(claims.getSubject()),
                    UserRole.from(claims.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public String parseAndGetSubject(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(jwtConfig.getIssuer())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
