package gg.popn.application.user.port.in.command;

import gg.popn.domain.user.model.field.IsHidden;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.Comment;

public record ModifyUserCommand(Password password, Password newPassword, IsHidden isHidden, Comment comment) {
}