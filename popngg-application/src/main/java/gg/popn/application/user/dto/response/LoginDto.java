package gg.popn.application.user.dto.response;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class LoginDto {
    String poptomoId;
    String token;


}
