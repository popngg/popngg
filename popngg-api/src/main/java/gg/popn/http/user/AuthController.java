package gg.popn.http.user;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.http.user.request.LoginRequest;
import gg.popn.http.user.request.PasswordResetConfirmRequest;
import gg.popn.http.user.request.PasswordResetRequest;
import gg.popn.http.user.response.LoginResponse;
import gg.popn.application.auth.dto.command.LoginCommand;
import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.dto.command.ConfirmPasswordResetCommand;
import gg.popn.application.auth.dto.command.RequestPasswordResetCommand;
import gg.popn.application.auth.port.in.PasswordResetUseCase;
import gg.popn.http.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"https://popn.gg", "https://api.popn.gg"})
@Tag(name = "Auth", description = "User operations")
public class AuthController {
    private final AuthenticateUserUseCase authenticateUser;
    private final PasswordResetUseCase passwordReset;

    @PostMapping("/login")
    SuccessResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUser.login(new LoginCommand(
                PoptomoId.of(request.poptomoId()),
                Password.of(request.password())));
        return SuccessResponse.<LoginResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(LoginResponse.from(result))
                .build();
    }

    @PostMapping("/password-reset/request")
    SuccessResponse<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        passwordReset.request(new RequestPasswordResetCommand(request.email()));
        return SuccessResponse.<Void>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .build();
    }

    @PostMapping("/password-reset/confirm")
    SuccessResponse<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordReset.confirm(new ConfirmPasswordResetCommand(
                request.token(),
                request.newPassword()));
        return SuccessResponse.<Void>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
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
