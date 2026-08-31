package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMedalCorrectionMigrationTest extends MySqlIntegrationTestSupport {

    @Test
    void correctsOnlyUnrenewedLegacyRowsAndRebuildsCachedPotential() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("15").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        seed(jdbc);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .load().migrate();

        assertThat(jdbc.queryForList("""
                SELECT medal_code FROM playdata ORDER BY playdata_id
                """, Integer.class)).containsExactly(11, 8, 9, 10, 13, 9);
        assertThat(jdbc.queryForObject("""
                SELECT potential_popclass FROM playdata WHERE playdata_id = 1
                """, Integer.class)).isNotEqualTo(123);
        assertThat(jdbc.queryForObject("""
                SELECT potential_popclass FROM user_profiles WHERE user_id = 1
                """, Integer.class)).isNotEqualTo(456);
    }

    private static void seed(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO users
                    (user_id, poptomo_id, password_hash, role, created_at, updated_at)
                VALUES
                    (1, '0000-0000-0001', 'x', 'USER', NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO user_profiles
                    (user_id, user_name, character_name, comment, is_hidden,
                     display_popclass, potential_popclass, legacy_popclass,
                     created_at, updated_at)
                VALUES (1, 'legacy', '', '', FALSE, 0, 456, 0, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO songs
                    (song_id, song_hash, genre_name, song_name, version, created_at, updated_at)
                VALUES (1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'genre', 'song', 28, NOW(), NOW())
                """);
        for (int chartId = 1; chartId <= 6; chartId++) {
            jdbc.update("""
                    INSERT INTO charts
                        (chart_id, song_id, difficulty_code, difficulty_label, level,
                         chart_version, is_deleted, created_at, updated_at)
                    VALUES (?, 1, ?, ?, 49, 28, FALSE, NOW(), NOW())
                    """, chartId, chartId, "D" + chartId);
        }
        jdbc.update("""
                INSERT INTO playdata
                    (playdata_id, user_id, chart_id, current_version,
                     version_score, version_score_known, all_time_score,
                     all_time_score_version, medal_code, popclass,
                     potential_popclass, last_renew_log_id, created_at, updated_at)
                VALUES
                    (1, 1, 1, 29, 0, FALSE, 90000, 28, 8, 0, 123, NULL, NOW(), NOW()),
                    (2, 1, 2, 29, 0, FALSE, 90000, 28, 9, 0, 123, NULL, NOW(), NOW()),
                    (3, 1, 3, 29, 0, FALSE, 90000, 28, 10, 0, 123, NULL, NOW(), NOW()),
                    (4, 1, 4, 29, 0, FALSE, 90000, 28, 11, 0, 123, NULL, NOW(), NOW()),
                    (5, 1, 5, 29, 0, FALSE, 90000, 28, 0, 0, 123, NULL, NOW(), NOW()),
                    (6, 1, 6, 29, 90000, FALSE, 90000, 29, 9, 123, 123, 99, NOW(), NOW())
                """);
    }
}
