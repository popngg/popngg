package gg.popn.application.auth.port.out;

import gg.popn.application.auth.model.AuthPrincipal;
import java.time.Duration;

public interface TokenIssuerPort {
    String issueAccessToken(AuthPrincipal principal, Duration ttl);
}
