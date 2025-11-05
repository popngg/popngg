package gg.popn.application.auth.port.out;

public interface PasswordHasherPort {
    String hash(String raw);
    boolean matches(String raw, String hashed);
}
