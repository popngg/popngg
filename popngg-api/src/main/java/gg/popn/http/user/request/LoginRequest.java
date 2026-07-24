package gg.popn.http.user.request;

import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import lombok.Value;

@Value
public class LoginRequest {
    PoptomoId poptomoId;
    Password password;
}
