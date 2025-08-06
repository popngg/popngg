package gg.popn.controller.user;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.user.application.dto.response.UserProfileDto;
import gg.popn.domain.user.application.dto.response.UserRankingsDto;
import gg.popn.domain.user.application.dto.request.ModifyUserRequest;
import gg.popn.model.response.SuccessResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/user")
@Tag(name = "User", description = "User operations")
public class UserController {

    @GetMapping("/profile/{poptomoId}")
    SuccessResponse<UserProfileDto> getUser(@PathVariable String poptomoId,
                                            @AuthenticationPrincipal User user) {
        return SuccessResponse.<UserProfileDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @PatchMapping("/profile/{poptomoId}")
    SuccessResponse<UserProfileDto> updateUser(@PathVariable String poptomoId,
                                               @RequestBody ModifyUserRequest request,
                                               @AuthenticationPrincipal User user) {
        return SuccessResponse.<UserProfileDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @PostMapping("/ranking")
    SuccessResponse<UserRankingsDto> getUserRankings(@AuthenticationPrincipal User user) {
        return SuccessResponse.<UserRankingsDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }
}
