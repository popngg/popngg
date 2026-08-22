package gg.popn.application.user.service;

import gg.popn.application.auth.port.out.CurrentPrincipalPort;
import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserProfileQuery;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.dto.result.UserRankingResult;
import gg.popn.application.user.dto.result.UserListResult;
import gg.popn.application.user.exception.UserProfileNotFoundException;
import gg.popn.application.user.port.in.UserProfileUseCase;
import gg.popn.application.user.port.out.UserProfilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService implements UserProfileUseCase {
    private final UserProfilePort profilePort;
    private final CurrentPrincipalPort currentPrincipalPort;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResult get(UserProfileQuery query) {
        var profile = profilePort.findByPoptomoId(query.poptomoId())
                .orElseThrow(UserProfileNotFoundException::new);
        if (profile.hidden() && !canManage(query.poptomoId())) {
            throw new UserProfileNotFoundException();
        }
        return profile;
    }

    @Override
    @Transactional
    public UserProfileResult update(UpdateUserProfileCommand command) {
        if (!canManage(command.poptomoId())) {
            throw new AccessDeniedException("Profile update is not allowed.");
        }
        profilePort.findByPoptomoId(command.poptomoId())
                .orElseThrow(UserProfileNotFoundException::new);
        return profilePort.update(command);
    }

    @Override
    @Transactional(readOnly = true)
    public UserRankingResult rankings(UserRankingQuery query) {
        var result = profilePort.findRankings(query);
        return new UserRankingResult(
                result.users(),
                query.page(),
                query.size(),
                result.totalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResult findUsers(FindUsersQuery query) {
        return profilePort.findUsers(query);
    }

    private boolean canManage(String poptomoId) {
        return currentPrincipalPort.get()
                .map(principal -> principal.getPoptomoId().getValue().equals(poptomoId)
                        || "ADMIN".equals(principal.getUserRole().getValue()))
                .orElse(false);
    }
}
