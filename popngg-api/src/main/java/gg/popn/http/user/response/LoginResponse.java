package gg.popn.http.user.response;

import gg.popn.application.auth.dto.response.LoginResult;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        UserProfileResponse profile
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresInSeconds(),
                result.role(),
                new UserProfileResponse(
                        result.profile().poptomoId(),
                        result.profile().userName()));
    }

    public record UserProfileResponse(String poptomoId, String userName) {
    }
}
