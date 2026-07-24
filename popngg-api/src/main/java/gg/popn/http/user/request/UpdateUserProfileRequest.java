package gg.popn.http.user.request;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 64) String userName,
        @Size(max = 128) String characterName,
        @Size(max = 255) String comment,
        @Size(max = 512) String profileImageUrl,
        Boolean hidden
) {
}
