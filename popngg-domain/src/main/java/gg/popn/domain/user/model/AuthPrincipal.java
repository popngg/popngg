package gg.popn.domain.user.model;

import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class AuthPrincipal {
    PoptomoId poptomoId;
    UserRole userRole;

    public static AuthPrincipal of(PoptomoId poptomoId, UserRole userRole) {
        return new AuthPrincipal(poptomoId, userRole);
    }
}
