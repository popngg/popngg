package gg.popn.application.auth.port.out;

public record IssuedAccessToken(String value, long expiresInSeconds) {
}
