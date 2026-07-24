package gg.popn.application.user.dto.response;

import gg.popn.domain.playdata.model.field.Medal;
import gg.popn.domain.playdata.model.field.Popclass;
import gg.popn.domain.playdata.model.field.Rank;
import gg.popn.domain.playdata.model.field.Score;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserPopclass;
import gg.popn.domain.user.model.field.Username;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingDataDto {
    private Username username;
    private UserPopclass userPopclass;
    private PoptomoId poptomoId;
    private Integer ranking;
    private Score score;
    private Medal medal;
    private Rank rank;
    private Popclass popclass;
}
