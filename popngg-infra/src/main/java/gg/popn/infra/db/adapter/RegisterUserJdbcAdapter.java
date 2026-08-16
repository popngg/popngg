package gg.popn.infra.db.adapter;

import gg.popn.application.auth.exception.AlreadyRegisteredException;
import gg.popn.application.auth.port.out.RegisterUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RegisterUserJdbcAdapter implements RegisterUserPort {
    private final JdbcTemplate jdbc;

    @Override
    public boolean exists(String poptomoId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE poptomo_id = ?", Integer.class, poptomoId);
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void create(String poptomoId, String passwordHash, boolean hidden) {
        try {
            KeyHolder keys = new GeneratedKeyHolder();
            Timestamp now = Timestamp.from(Instant.now());
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (poptomo_id, password_hash, email, role, created_at, updated_at)
                        VALUES (?, ?, NULL, 'USER', ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, poptomoId);
                statement.setString(2, passwordHash);
                statement.setTimestamp(3, now);
                statement.setTimestamp(4, now);
                return statement;
            }, keys);
            if (keys.getKey() == null) throw new IllegalStateException("user id was not generated");
            jdbc.update("""
                    INSERT INTO user_profiles
                        (user_id, user_name, character_name, comment, profile_image_url, is_hidden,
                         display_popclass, potential_popclass, legacy_popclass, normal_credit,
                         extra_credit, time_play_10_credit, time_play_16_credit, created_at, updated_at)
                    VALUES (?, ?, '', '', NULL, ?, 0, 0, 0, 0, 0, 0, 0, ?, ?)
                    """, keys.getKey().longValue(), poptomoId, hidden, now, now);
        } catch (DuplicateKeyException exception) {
            throw new AlreadyRegisteredException();
        }
    }
}
