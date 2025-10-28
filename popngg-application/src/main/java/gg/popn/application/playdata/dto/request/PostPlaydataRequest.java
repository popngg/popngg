package gg.popn.application.playdata.dto.request;

import gg.popn.application.playdata.dto.PlaydataInputDto;
import gg.popn.domain.user.model.field.BattleCredit;
import gg.popn.domain.user.model.field.Character;
import gg.popn.domain.user.model.field.Comment;
import gg.popn.domain.user.model.field.IsHidden;
import gg.popn.domain.user.model.field.LocalCredit;
import gg.popn.domain.user.model.field.NormalCredit;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.Username;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostPlaydataRequest {
    Username userName;
    PoptomoId poptomoId;
    Character character;
    Comment comment;
    IsHidden isHidden;
    Password password;
    NormalCredit normalCredit;
    BattleCredit battleCredit;
    LocalCredit localCredit;
    List<PlaydataInputDto> playdataInputDtos;
}
