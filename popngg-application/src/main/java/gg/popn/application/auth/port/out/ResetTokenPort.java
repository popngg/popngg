package gg.popn.application.auth.port.out;

public interface ResetTokenPort {
    String generate();

    String hash(String rawToken);
}
