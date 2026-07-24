package gg.popn.infra.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class TransitionalPasswordVerificationAdapterTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final TransitionalPasswordVerificationAdapter adapter =
            new TransitionalPasswordVerificationAdapter(encoder);

    @Test
    void verifiesBcryptValues() {
        String stored = encoder.encode("presented");

        assertThat(adapter.matches("presented", stored)).isTrue();
        assertThat(adapter.matches("different", stored)).isFalse();
    }

    @Test
    void isolatesLegacyDirectComparison() {
        assertThat(adapter.matches("legacy-value", "legacy-value")).isTrue();
        assertThat(adapter.matches("different", "legacy-value")).isFalse();
    }
}
