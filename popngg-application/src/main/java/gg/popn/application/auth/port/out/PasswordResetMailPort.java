package gg.popn.application.auth.port.out;

public interface PasswordResetMailPort {
    void sendResetLink(String email, String rawToken);
}
