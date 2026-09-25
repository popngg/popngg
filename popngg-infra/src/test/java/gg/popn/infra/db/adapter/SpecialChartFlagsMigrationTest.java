package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SpecialChartFlagsMigrationTest extends MySqlIntegrationTestSupport {

    @Test
    void importsCuratedChartFlagsAndExtraMusicCategories() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("25").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);

        insertSong(jdbc, 1, "ordinary");
        insertSong(jdbc, 2, "ordinary two");
        insertSong(jdbc, 1962, "Asian Trinity");
        insertSong(jdbc, 2028, "精霊都市リトラ・ミュネ");
        insertChart(jdbc, 29, 1);
        insertChart(jdbc, 783, 2);

        flyway.migrate();

        assertThat(jdbc.queryForObject(
                "SELECT has_strict_judgement FROM charts WHERE chart_id = 29", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT has_strict_gauge FROM charts WHERE chart_id = 783", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT extra_type FROM songs WHERE song_id = 1962", String.class))
                .isEqualTo("EXTRA");
        assertThat(jdbc.queryForObject(
                "SELECT extra_type FROM songs WHERE song_id = 2028", String.class))
                .isEqualTo("SUPER_EXTRA");
        assertThat(jdbc.queryForObject(
                "SELECT extra_type FROM songs WHERE song_id = 1", String.class))
                .isEqualTo("NONE");
    }

    private static void insertSong(JdbcTemplate jdbc, long songId, String songName) {
        jdbc.update("""
                INSERT INTO songs
                    (song_id, song_hash, genre_name, song_name, version, created_at, updated_at)
                VALUES (?, ?, 'genre', ?, 29, NOW(), NOW())
                """, songId, "%032d".formatted(songId), songName);
    }

    private static void insertChart(JdbcTemplate jdbc, long chartId, long songId) {
        jdbc.update("""
                INSERT INTO charts
                    (chart_id, song_id, difficulty_code, difficulty_label, level,
                     chart_version, created_at, updated_at)
                VALUES (?, ?, 4, 'EX', 49, 29, NOW(), NOW())
                """, chartId, songId);
    }
}
