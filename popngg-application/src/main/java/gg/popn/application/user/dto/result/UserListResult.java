package gg.popn.application.user.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record UserListResult(
        List<Item> users, int page, int size, long totalElements
) {
    public record Item(
            String poptomoId,
            String userName,
            String profileImageUrl,
            String comment,
            int rank,
            int displayPopclass,
            List<UserProfileResult.MedalSummary> bestLevels,
            LocalDateTime updatedAt
    ) {
    }
}
