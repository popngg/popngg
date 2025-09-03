package gg.popn.domain.user.application.dto.response;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class LoginResponseDto {
    String poptomoId;
    String token;


}
