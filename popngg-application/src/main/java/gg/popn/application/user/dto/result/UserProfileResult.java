package gg.popn.application.user.dto.result;

public record UserProfileResult(
        String poptomoId,
        String userName,
        String characterName,
        String comment,
        String profileImageUrl,
        boolean hidden,
        int displayPopclass,
        int potentialPopclass,
        int legacyPopclass,
        int normalCredit,
        int extraCredit,
        int timePlay10Credit,
        int timePlay16Credit
) {
}
