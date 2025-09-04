package gg.popn.domain.user.application.dto.request;


import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import lombok.Value;


@Value
public class UpdatePasswordRequest {

    PoptomoId poptomoId;
    Password password;

}
