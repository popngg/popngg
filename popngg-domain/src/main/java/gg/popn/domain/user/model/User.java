package gg.popn.domain.user.model;

import gg.popn.domain.user.model.field.IsHidden;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.domain.user.model.field.Username;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class User {
    Username username;
    PoptomoId poptomoId;
    Integer popclass;
    String character;
    String comment;
    IsHidden isHidden;
    UserRole role;
}
