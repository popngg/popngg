package gg.popn.application.auth.dto.response;

public record LoginResult(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        UserProfileSummary profile
) {
    public record UserProfileSummary(String poptomoId, String userName) {
    }
}
