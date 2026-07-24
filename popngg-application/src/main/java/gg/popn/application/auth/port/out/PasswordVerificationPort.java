package gg.popn.application.auth.port.out;

public interface PasswordVerificationPort {
    boolean matches(String presentedPassword, String storedPassword);
}
