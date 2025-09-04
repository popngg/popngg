package gg.popn.domain.user.application.dto.response;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UserProfileDto {
    String username;
    String poptomoId;
    Integer popclass;
    String character;
    String comment;
    Integer isHidden;
    String role;

    String maxClearLevel;
    String maxFullComboLevel;
    String maxPerfectLevel;
    Integer playedChartCount;

    UserProfileDto of(gg.popn.domain.user.model.UserProfile userProfile) {
        return UserProfileDto.builder()
                .username(userProfile.getUsername().getValue())
                .poptomoId(userProfile.getPoptomoId().getValue())
                .popclass(userProfile.getPopclass().getValue())
                .character(userProfile.getCharacter())
                .comment(userProfile.getComment())
                .isHidden(userProfile.getIsHidden().getValue())
                .role(userProfile.getRole().getValue())
                .maxClearLevel(userProfile.getMaxClearLevel())
                .maxFullComboLevel(userProfile.getMaxFullComboLevel())
                .maxPerfectLevel(userProfile.getMaxPerfectLevel())
                .playedChartCount(userProfile.getPlayedChartCount())
                .build();
    }
}
