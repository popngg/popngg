
package gg.popn.domain.user.application.dto.response;

import gg.popn.domain.user.application.dto.UserRankingDto;
import gg.popn.domain.user.model.User;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserPopclass;
import gg.popn.domain.user.model.field.Username;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Builder
@Value
public class UserRankingsDto {
    public List<UserRankingDto> userRankings;

    UserRankingDto of(List<User> users) {
        return null; // TODO: implement
    }
}
