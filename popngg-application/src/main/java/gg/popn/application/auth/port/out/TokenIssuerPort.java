package gg.popn.application.auth.port.out;


import gg.popn.domain.user.model.AuthPrincipal;

import java.time.Duration;

public interface  TokenIssuerPort {
    String issueAccessToken(AuthPrincipal principal, Duration ttl);
    String parseAndGetSubject(String token);
}
