package gg.popn.http.user.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthSessionResponse(
        String poptomoId,
        String userName,
        @Schema(nullable = true) String avatarUrl
) {
}
