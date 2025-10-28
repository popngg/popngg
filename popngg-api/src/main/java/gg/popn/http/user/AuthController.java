package gg.popn.http.user;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.application.user.dto.request.LoginRequest;
import gg.popn.application.user.dto.response.LoginDto;
import gg.popn.http.common.response.SuccessResponse;
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
    SuccessResponse<LoginDto> login(@RequestBody LoginRequest request) {
        return SuccessResponse.<LoginDto>builder()
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
