package gg.popn.http.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}$") String poptomoId,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{64}$") String password,
        @NotNull Boolean hidden
) {
}
