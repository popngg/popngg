package gg.popn.infra.security.adapter;


import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.application.auth.port.out.IssuedAccessToken;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.infra.security.config.JwtConfig;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider implements TokenPort {

    private final JwtConfig jwtConfig;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public IssuedAccessToken issueAccessToken(AuthPrincipal principal) {
        Duration ttl = Duration.ofHours(jwtConfig.getExpirationHours());
        String value = Jwts.builder()
                .setSubject(principal.getPoptomoId().getValue())  // 유저의 poptomoId를 subject로 설정
                .setIssuer(jwtConfig.getIssuer())  // issuer 설정
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttl.toMillis()))  // 만료시간 설정
                .claim("role", principal.getUserRole().getValue())  // role은 claim에 추가
                .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecretKey())  // 서명 알고리즘 및 비밀키 설정
                .compact();
        return new IssuedAccessToken(value, ttl.toSeconds());
    }

    @Override
    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtConfig.getSecretKey())  // 비밀키로 서명 검증
                    .parseClaimsJws(token)
                    .getBody();
            String poptomoId = claims.getSubject();  // poptomoId는 subject로 설정
            String role = claims.get("role", String.class);  // role을 claim에서 가져옴
            return Optional.of(AuthPrincipal.of(PoptomoId.of(poptomoId), UserRole.from(role)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();  // 토큰이 유효하지 않으면 빈 Optional 반환
        }
    }

    @Override
    public String parseAndGetSubject(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtConfig.getSecretKey())  // 비밀키로 서명 검증
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();  // Subject(유저 ID) 반환
        } catch (JwtException | IllegalArgumentException e) {
            return null;  // 유효하지 않은 토큰이면 null 반환
        }
    }
}
