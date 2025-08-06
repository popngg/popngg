package gg.popn.domain.user.application.dto.request;

import gg.popn.domain.user.model.field.IsHidden;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.Comment;
import lombok.Value;

@Value
public class ModifyUserRequest {
    Password password;
    Password newPassword;
    Comment comment;
    IsHidden isHidden;
}
