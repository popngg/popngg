package gg.popn.application.user.port.out;

import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.dto.result.UserListResult;
import java.util.List;
import java.util.Optional;

public interface UserProfilePort {
    Optional<UserProfileResult> findByPoptomoId(String poptomoId);

    UserProfileResult update(UpdateUserProfileCommand command);

    RankingPage findRankings(UserRankingQuery query);

    UserListResult findUsers(FindUsersQuery query);

    record RankingPage(List<UserProfileResult> users, long totalElements) {
    }
}
