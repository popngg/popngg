package gg.popn.http.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetConfirmRequest(
        @NotBlank
        String token,
        @NotBlank
        @Pattern(regexp = "^[a-f0-9]{64}$")
        String newPassword
) {
}
