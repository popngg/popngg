package gg.popn.infra.db.adapter;

import gg.popn.application.auth.exception.AlreadyRegisteredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterUserJdbcAdapterTest {
    private JdbcTemplate jdbc;
    private RegisterUserJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:register;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE users(user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  poptomo_id VARCHAR(32) UNIQUE NOT NULL, password_hash VARCHAR(255) NOT NULL,
                  email VARCHAR(255), role VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL,
                  updated_at TIMESTAMP NOT NULL)
                """);
        jdbc.execute("""
                CREATE TABLE user_profiles(user_id BIGINT PRIMARY KEY, user_name VARCHAR(64) NOT NULL,
                  character_name VARCHAR(128) NOT NULL, comment VARCHAR(255) NOT NULL,
                  profile_image_url VARCHAR(512), is_hidden BOOLEAN NOT NULL,
                  display_popclass INT NOT NULL, potential_popclass INT NOT NULL,
                  legacy_popclass INT NOT NULL, normal_credit INT NOT NULL, extra_credit INT NOT NULL,
                  time_play_10_credit INT NOT NULL, time_play_16_credit INT NOT NULL,
                  created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)
                """);
        adapter = new RegisterUserJdbcAdapter(jdbc);
    }

    @Test
    void createsAccountAndHiddenProfile() {
        assertThat(adapter.exists("1234-5678-9012")).isFalse();

        adapter.create("1234-5678-9012", "bcrypt", true);

        assertThat(adapter.exists("1234-5678-9012")).isTrue();
        assertThat(jdbc.queryForMap("SELECT poptomo_id,password_hash,role FROM users"))
                .containsEntry("poptomo_id", "1234-5678-9012")
                .containsEntry("password_hash", "bcrypt").containsEntry("role", "USER");
        assertThat(jdbc.queryForMap("SELECT user_name,is_hidden FROM user_profiles"))
                .containsEntry("user_name", "1234-5678-9012").containsEntry("is_hidden", true);
    }

    @Test
    void translatesDuplicateIdToDomainException() {
        adapter.create("1234-5678-9012", "first", false);
        assertThatThrownBy(() -> adapter.create("1234-5678-9012", "second", false))
                .isInstanceOf(AlreadyRegisteredException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }
}
