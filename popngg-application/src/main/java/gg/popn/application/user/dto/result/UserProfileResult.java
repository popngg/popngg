package gg.popn.application.user.dto.result;

import java.time.LocalDateTime;
import java.util.List;

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
        int timePlay16Credit,
        List<MedalSummary> medalSummaries,
        LocalDateTime updatedAt
) {
    public UserProfileResult(
            String poptomoId, String userName, String characterName, String comment,
            String profileImageUrl, boolean hidden, int displayPopclass,
            int potentialPopclass, int legacyPopclass, int normalCredit,
            int extraCredit, int timePlay10Credit, int timePlay16Credit) {
        this(poptomoId, userName, characterName, comment, profileImageUrl, hidden,
                displayPopclass, potentialPopclass, legacyPopclass, normalCredit,
                extraCredit, timePlay10Credit, timePlay16Credit, List.of(), null);
    }

    public record MedalSummary(
            String kind,
            int maxLevel,
            long achieved,
            long total
    ) {
    }
}
