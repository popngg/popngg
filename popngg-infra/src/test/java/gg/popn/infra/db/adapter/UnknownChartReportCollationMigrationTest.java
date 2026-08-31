package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownChartReportCollationMigrationTest extends MySqlIntegrationTestSupport {

    @Test
    void normalizesImportedSongCollationForUnknownReportLookups() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("18").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                ALTER TABLE songs
                    MODIFY song_name VARCHAR(255)
                        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                    MODIFY genre_name VARCHAR(255)
                        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL
                """);
        jdbc.update("""
                INSERT INTO songs
                    (song_id, song_hash, genre_name, song_name, version,
                     created_at, updated_at)
                VALUES (1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'genre', 'known', 28,
                        NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO charts
                    (chart_id, song_id, difficulty_code, difficulty_label, level,
                     chart_version, is_upper, created_at, updated_at)
                VALUES (1, 1, 4, 'EX', 48, 28, FALSE, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO unknown_chart_reports
                    (renew_log_id, poptomo_id, song_name, genre_name, artist_name,
                     difficulty_code, is_upper, occurrences, resolved,
                     first_seen_at, last_seen_at)
                VALUES
                    (1, 'user', 'known', 'genre', 'reported artist', 4, FALSE, 1, FALSE, NOW(), NOW()),
                    (1, 'user', 'unknown', 'genre', '', 4, FALSE, 1, FALSE, NOW(), NOW())
                """);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .load().migrate();

        var adapter = new UnknownChartReportJdbcAdapter(jdbc);
        assertThat(adapter.findRecentUnresolved(10))
                .singleElement().satisfies(report ->
                        assertThat(report.songName()).isEqualTo("unknown"));
        assertThat(adapter.findRecentIncomplete(10))
                .singleElement().satisfies(report ->
                        assertThat(report.songName()).isEqualTo("known"));
    }
}
