package gg.popn.http.user.response;

import gg.popn.application.user.dto.result.UserProfileResult;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
        String poptomoId,
        String userName,
        String characterName,
        String comment,
        String profileImageUrl,
        boolean hidden,
        int displayPopclass,
        int potentialPopclass,
        int legacyPopclass,
        CreditsResponse credits,
        List<MedalSummaryResponse> medalSummaries,
        LocalDateTime updatedAt
) {
    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.poptomoId(),
                result.userName(),
                result.characterName(),
                result.comment(),
                result.profileImageUrl(),
                result.hidden(),
                result.displayPopclass(),
                result.potentialPopclass(),
                result.legacyPopclass(),
                new CreditsResponse(
                        result.normalCredit(),
                        result.extraCredit(),
                        result.timePlay10Credit(),
                        result.timePlay16Credit()),
                result.medalSummaries().stream()
                        .map(MedalSummaryResponse::from)
                        .toList(),
                result.updatedAt());
    }

    public record CreditsResponse(
            int normal,
            int extra,
            int timePlay10,
            int timePlay16
    ) {
    }

    public record MedalSummaryResponse(
            String kind,
            int maxLevel,
            long achieved,
            long total
    ) {
        private static MedalSummaryResponse from(UserProfileResult.MedalSummary summary) {
            return new MedalSummaryResponse(summary.kind(), summary.maxLevel(),
                    summary.achieved(), summary.total());
        }
    }
}
