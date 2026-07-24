package gg.popn.infra.db.mapper;

import gg.popn.domain.user.model.User;
import gg.popn.domain.user.model.field.IsHidden;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.domain.user.model.field.Username;
import gg.popn.infra.db.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toDomain(UserEntity e) {
        return User.builder()
                .username(Username.from(e.getProfile().getUserName()))
                .poptomoId(PoptomoId.of(e.getPoptomoId()))
                .popclass(e.getProfile().getDisplayPopclass())
                .character(e.getProfile().getCharacterName())
                .comment(e.getProfile().getComment())
                .isHidden(IsHidden.of(e.getProfile().isHidden() ? 1 : 0))
                .role(UserRole.of(e.getRole()))
                .build();
    }
}
