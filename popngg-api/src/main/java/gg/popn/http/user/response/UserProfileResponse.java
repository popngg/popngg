package gg.popn.http.user.response;

import gg.popn.application.user.dto.result.UserProfileResult;

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
        CreditsResponse credits
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
                        result.timePlay16Credit()));
    }

    public record CreditsResponse(
            int normal,
            int extra,
            int timePlay10,
            int timePlay16
    ) {
    }
}
