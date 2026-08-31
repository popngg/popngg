package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyRankCorrectionMigrationTest extends MySqlIntegrationTestSupport {

    @Test
    void mapsCompactLegacyRanksAndLeavesRenewedRowsUntouched() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("16").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        seed(jdbc);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .load().migrate();

        assertThat(jdbc.queryForList("""
                SELECT all_time_rank_code FROM playdata ORDER BY playdata_id
                """, Integer.class))
                .containsExactly(13, 2, 3, 5, 7, 9, 10, 11, 12, 1);
    }

    private static void seed(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO users
                    (user_id, poptomo_id, password_hash, role, created_at, updated_at)
                VALUES (1, '0000-0000-0001', 'x', 'USER', NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO user_profiles
                    (user_id, user_name, character_name, comment, is_hidden,
                     display_popclass, potential_popclass, legacy_popclass,
                     created_at, updated_at)
                VALUES (1, 'legacy', '', '', FALSE, 0, 0, 0, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO songs
                    (song_id, song_hash, genre_name, song_name, version, created_at, updated_at)
                VALUES (1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'genre', 'song', 28, NOW(), NOW())
                """);
        for (int chartId = 1; chartId <= 10; chartId++) {
            jdbc.update("""
                    INSERT INTO charts
                        (chart_id, song_id, difficulty_code, difficulty_label, level,
                         chart_version, is_deleted, created_at, updated_at)
                    VALUES (?, 1, ?, ?, 45, 28, FALSE, NOW(), NOW())
                    """, chartId, chartId, "D" + chartId);
        }
        for (int rank = 0; rank <= 8; rank++) {
            jdbc.update("""
                    INSERT INTO playdata
                        (playdata_id, user_id, chart_id, current_version,
                         version_score, version_score_known, all_time_score,
                         all_time_score_version, all_time_rank_code, medal_code,
                         last_renew_log_id, created_at, updated_at)
                    VALUES (?, 1, ?, 29, 0, FALSE, 98193, 28, ?, 5,
                            NULL, NOW(), NOW())
                    """, rank + 1, rank + 1, rank);
        }
        jdbc.update("""
                INSERT INTO playdata
                    (playdata_id, user_id, chart_id, current_version,
                     version_score, version_score_known, all_time_score,
                     all_time_score_version, all_time_rank_code, medal_code,
                     last_renew_log_id, created_at, updated_at)
                VALUES (10, 1, 10, 29, 98193, FALSE, 98193, 29, 1, 5,
                        99, NOW(), NOW())
                """);
    }
}
