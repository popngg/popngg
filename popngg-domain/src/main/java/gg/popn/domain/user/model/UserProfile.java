package gg.popn.domain.user.model;

import gg.popn.domain.user.model.field.*;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UserProfile {
    Username username;
    PoptomoId poptomoId;
    UserPopclass popclass;
    String character;
    String comment;
    IsHidden isHidden;
    UserRole role;

    String maxClearLevel;
    String maxFullComboLevel;
    String maxPerfectLevel;
    Integer playedChartCount;
}
