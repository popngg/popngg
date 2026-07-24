package gg.popn.application.auth.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("The password reset token is invalid or expired.");
    }
}
