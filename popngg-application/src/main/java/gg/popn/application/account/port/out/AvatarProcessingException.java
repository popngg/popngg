package gg.popn.application.account.port.out;

public class AvatarProcessingException extends RuntimeException {
    public AvatarProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public AvatarProcessingException(String message) {
        super(message);
    }
}
