package gg.popn.domain.user.application.dto.request;

import lombok.Value;

@Value
public class ModifyUserRequest {
    String password;
    String newPassword;
    String comment;
    Integer isHidden;
}
