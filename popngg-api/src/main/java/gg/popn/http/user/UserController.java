package gg.popn.http.user;

import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserProfileQuery;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.port.in.UserProfileUseCase;
import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.common.response.PageResponse;
import gg.popn.http.user.request.UpdateUserProfileRequest;
import gg.popn.http.user.response.UserProfileResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserProfileUseCase userProfile;

    @GetMapping("/{poptomoId}")
    SuccessResponse<UserProfileResponse> getUser(
            @PathVariable
            @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}$")
            String poptomoId) {
        return success(UserProfileResponse.from(
                userProfile.get(new UserProfileQuery(poptomoId))));
    }

    @PatchMapping("/{poptomoId}")
    SuccessResponse<UserProfileResponse> updateUser(
            @PathVariable
            @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}$")
            String poptomoId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        var result = userProfile.update(new UpdateUserProfileCommand(
                poptomoId,
                request.userName(),
                request.characterName(),
                request.comment(),
                request.profileImageUrl(),
                request.hidden()));
        return success(UserProfileResponse.from(result));
    }

    @GetMapping("/rankings")
    SuccessResponse<PageResponse<UserProfileResponse>> getUserRankings(
            @RequestParam(defaultValue = "displayPopclass") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = userProfile.rankings(new UserRankingQuery(
                UserRankingQuery.Sort.fromApiValue(sort),
                page,
                size));
        return success(PageResponse.of(
                result.users().stream().map(UserProfileResponse::from).toList(),
                result.totalElements(), result.page(), result.size()));
    }

    private static <T> SuccessResponse<T> success(T data) {
        return SuccessResponse.<T>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(data)
                .build();
    }
}
