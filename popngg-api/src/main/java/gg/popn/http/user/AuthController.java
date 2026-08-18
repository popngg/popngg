package gg.popn.http.user;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.http.user.request.LoginRequest;
import gg.popn.http.user.request.PasswordResetConfirmRequest;
import gg.popn.http.user.request.PasswordResetRequest;
import gg.popn.http.user.response.LoginResponse;
import gg.popn.http.user.response.AuthSessionResponse;
import gg.popn.infra.security.CustomUserPrincipal;
import gg.popn.application.auth.dto.command.LoginCommand;
import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.port.in.RegisterUserUseCase;
import gg.popn.application.auth.dto.command.RegisterCommand;
import gg.popn.application.auth.dto.command.ConfirmPasswordResetCommand;
import gg.popn.application.auth.dto.command.RequestPasswordResetCommand;
import gg.popn.application.auth.port.in.PasswordResetUseCase;
import gg.popn.application.user.dto.query.UserProfileQuery;
import gg.popn.application.user.port.in.UserProfileUseCase;
import gg.popn.http.common.response.SuccessResponse;
import gg.popn.http.user.request.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "User operations")
public class AuthController {
    @Value("${popngg.auth.cookie-secure:true}")
    private boolean cookieSecure = true;

    private final AuthenticateUserUseCase authenticateUser;
    private final PasswordResetUseCase passwordReset;
    private final RegisterUserUseCase registerUser;
    private final UserProfileUseCase userProfile;

    @PostMapping("/login")
    SuccessResponse<Void> login(@Valid @RequestBody LoginRequest request,
                                HttpServletResponse response) {
        var result = authenticateUser.login(new LoginCommand(
                PoptomoId.of(request.poptomoId()),
                Password.of(request.password())));
        setAccessTokenCookie(response, result.accessToken(), result.expiresInSeconds());
        return SuccessResponse.<Void>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .build();
    }

    @GetMapping("/registrations/{poptomoId}")
    ResponseEntity<Void> registration(@PathVariable String poptomoId) {
        return registerUser.exists(poptomoId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/register")
    SuccessResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                            HttpServletResponse response) {
        var result = registerUser.register(new RegisterCommand(
                request.poptomoId(), request.password(), request.hidden()));
        setAccessTokenCookie(response, result.accessToken(), result.expiresInSeconds());
        return SuccessResponse.<LoginResponse>builder()
                .code(ResponseCode.SUCCESS).message(ResponseMessage.SUCCESS)
                .data(LoginResponse.from(result)).build();
    }

    private void setAccessTokenCookie(HttpServletResponse response, String token, long maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("access_token", token)
                .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/")
                .maxAge(maxAge).build().toString());
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

    @GetMapping("/session")
    SuccessResponse<AuthSessionResponse> session(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        AuthSessionResponse data = null;
        if (principal != null) {
            var profile = userProfile.get(new UserProfileQuery(
                    principal.getPoptomoId().getValue()));
            data = new AuthSessionResponse(
                    profile.poptomoId(), profile.userName(), profile.profileImageUrl());
        }
        return SuccessResponse.<AuthSessionResponse>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .data(data)
                .build();
    }

    @PostMapping("/logout")
    SuccessResponse<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("access_token", "")
                .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/")
                .maxAge(0).build().toString());
        return SuccessResponse.<Void>builder()
                .code(ResponseCode.SUCCESS)
                .message(ResponseMessage.SUCCESS)
                .build();
    }
}
