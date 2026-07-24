package gg.popn.http.user.response;

import gg.popn.application.user.dto.result.UserRankingResult;
import java.util.List;

public record UserRankingResponse(
        List<UserProfileResponse> users,
        int page,
        int size,
        long totalElements
) {
    public static UserRankingResponse from(UserRankingResult result) {
        return new UserRankingResponse(
                result.users().stream().map(UserProfileResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements());
    }
}
