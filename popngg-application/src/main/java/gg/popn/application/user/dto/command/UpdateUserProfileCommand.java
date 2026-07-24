package gg.popn.application.user.dto.command;

public record UpdateUserProfileCommand(
        String poptomoId,
        String userName,
        String characterName,
        String comment,
        String profileImageUrl,
        Boolean hidden
) {
}
