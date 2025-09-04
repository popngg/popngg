package gg.popn.domain.user.application.dto.response;

import gg.popn.domain.user.application.dto.UserRankingDto;
import gg.popn.domain.user.model.User;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class UserRankingsDto {
    public List<UserRankingDto> userRankings;

    UserRankingDto of(List<User> users) {
        return null; // TODO: implement
    }
}
