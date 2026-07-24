package gg.popn.application.user.dto.result;

import java.util.List;

public record UserRankingResult(
        List<UserProfileResult> users,
        int page,
        int size,
        long totalElements
) {
}
