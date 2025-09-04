package gg.popn.domain.playdata.application.dto.response;

import gg.popn.domain.playdata.application.dto.PlaydataDto;
import gg.popn.domain.playdata.model.field.Popclass;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.Username;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class UserWithPlaydataDto {
    Username username;
    PoptomoId poptomoId;
    Popclass popclass;
    Integer toNext;
    List<PlaydataDto> playdataList;
}
