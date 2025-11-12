// popngg-infra/src/main/java/gg/popn/infra/security/adapter/JwtTokenIssuerAdapter.java
package gg.popn.infra.security.adapter;

import gg.popn.application.auth.port.out.TokenIssuerPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.infra.security.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private final JwtConfig jwtConfig;

    @Override
    public String issueAccessToken(AuthPrincipal principal, java.time.Duration ttl) {
        return Jwts.builder()
                .signWith(new SecretKeySpec(jwtConfig.getSecretKey().getBytes(), SignatureAlgorithm.HS512.getJcaName()))
                .setSubject(principal.getPoptomoId().getValue() + ":" + principal.getUserRole().getValue())
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(ttl)))
                .compact();
    }

    @Override
    public String parseAndGetSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtConfig.getSecretKey().getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
