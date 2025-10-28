package gg.popn.application.user.dto.response;

import gg.popn.application.user.dto.UserRankingDto;
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
