package gg.popn.application.auth.dto.command;

public record ConfirmPasswordResetCommand(String token, String newPassword) {
}
