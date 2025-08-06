package gg.popn.domain.user.application.dto;

import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserPopclass;
import gg.popn.domain.user.model.field.Username;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Builder
@Value
public class UserRankingDto {
    Username username;
    PoptomoId poptomoId;
    UserPopclass popclass;
    String character;
    String comment;
    Date updatedAt;
    Integer ranking;
}
