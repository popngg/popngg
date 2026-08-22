package gg.popn.application.user.port.in;

import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserProfileQuery;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.dto.result.UserRankingResult;
import gg.popn.application.user.dto.result.UserListResult;

public interface UserProfileUseCase {
    UserProfileResult get(UserProfileQuery query);

    UserProfileResult update(UpdateUserProfileCommand command);

    UserRankingResult rankings(UserRankingQuery query);

    UserListResult findUsers(FindUsersQuery query);
}
