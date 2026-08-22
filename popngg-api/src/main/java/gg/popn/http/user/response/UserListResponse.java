package gg.popn.http.user.response;

import gg.popn.application.user.dto.result.UserListResult;

import java.time.LocalDateTime;
import java.util.List;

public record UserListResponse(
        String id,
        String name,
        String avatarUrl,
        String comment,
        int rank,
        int popnClass,
        List<BestLevelResponse> bestLevels,
        LocalDateTime updatedAt
) {
    public static UserListResponse from(UserListResult.Item item) {
        return new UserListResponse(
                item.poptomoId(), item.userName(), item.profileImageUrl(), item.comment(),
                item.rank(), item.displayPopclass(), item.bestLevels().stream()
                        .map(summary -> new BestLevelResponse(
                                summary.kind(), summary.maxLevel() == 0
                                        ? null : summary.maxLevel()))
                        .toList(), item.updatedAt());
    }

    public record BestLevelResponse(String kind, Integer maxLevel) {
    }
}
