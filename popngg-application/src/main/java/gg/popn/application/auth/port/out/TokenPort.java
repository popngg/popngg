package gg.popn.application.auth.port.out;


import gg.popn.domain.user.model.AuthPrincipal;

import java.time.Duration;
import java.util.Optional;

public interface TokenPort {

    // 토큰 발급
    String issueAccessToken(AuthPrincipal principal, Duration ttl);

    // 토큰에서 Subject(유저 ID)를 추출
    String parseAndGetSubject(String token);

    // 토큰에서 AuthPrincipal을 추출 (파싱)
    Optional<AuthPrincipal> parse(String token);
}