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
                .username(Username.of(e.getUserName()))
                .poptomoId(PoptomoId.of(e.getPoptomoId()))
                .popclass(e.getPopclass())
                .character(e.getCharacter())
                .comment(e.getComment())
                .isHidden(IsHidden.of(e.getIsHidden()))
                .role(UserRole.of(e.getRole()))
                .build();
    }
}