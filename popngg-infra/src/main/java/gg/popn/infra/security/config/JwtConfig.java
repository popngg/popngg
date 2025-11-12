package gg.popn.infra.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-hours}")
    private long expirationHours;

    public String getSecretKey() {
        return secretKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public long getExpirationHours() {
        return expirationHours;
    }
}