package gg.popn.infra.security.adapter;

import gg.popn.application.auth.port.out.PasswordVerificationPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TransitionalPasswordVerificationAdapter implements PasswordVerificationPort {
    private final PasswordEncoder passwordEncoder;

    public TransitionalPasswordVerificationAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean matches(String presentedPassword, String storedPassword) {
        if (presentedPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(presentedPassword, storedPassword);
        }
        return MessageDigest.isEqual(
                presentedPassword.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
