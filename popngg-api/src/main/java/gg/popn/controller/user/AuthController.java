package gg.popn.controller.user;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.user.application.dto.request.LoginRequestDto;
import gg.popn.domain.user.application.dto.response.LoginResponseDto;
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
@RequestMapping("/api/v2/auth")
@CrossOrigin(origins = "https://popn.gg, https://api.popn.gg")
@Tag(name = "Auth", description = "User operations")
public class AuthController {


    @PostMapping("/login")
    SuccessResponse<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        return SuccessResponse.<LoginResponseDto>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }

    @GetMapping("/check")
    SuccessResponse<Void> check(@AuthenticationPrincipal User user) {
        return SuccessResponse.<Void>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(null) // TODO: implement
                .build();
    }
}
