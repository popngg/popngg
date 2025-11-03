package gg.popn.infra.security;

import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import lombok.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenService {

    private final SecretKey key;
    private final long expirationHours;
    private final String issuer;

    public JwtTokenService(
            @Value("${security.jwt.secret-key}") String secretKey,
            @Value("${security.jwt.expiration-hours}") long expirationHours,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes()); // HS256/HS512 자동 결정
        this.expirationHours = expirationHours;
        this.issuer = issuer;
    }

    public String issueToken(PoptomoId poptomoId, String username, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(poptomoId.getValue())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .addClaims(Map.of(
                        "username", username,
                        "role", role.getValue() // "USER" or "ADMIN"
                ))
                .signWith(key, SignatureAlgorithm.HS256) // 또는 HS512
                .compact();
    }

    public Payload parse(String token) throws JwtException {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return new Payload(
                PoptomoId.of(claims.getSubject()),
                (String) claims.get("username"),
                UserRole.of((String) claims.get("role"))
        );
    }

    public record Payload(PoptomoId poptomoId, String username, UserRole role) {}
}