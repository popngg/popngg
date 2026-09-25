package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SpecialChartFlagsMigrationTest extends MySqlIntegrationTestSupport {

    @Test
    void importsCuratedValuesIntoANewTableWithoutChangingCatalogTables() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("25").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);

        insertSong(jdbc, 1, "ordinary");
        insertSong(jdbc, 2, "ordinary two");
        insertChart(jdbc, 29, 1);
        insertChart(jdbc, 783, 2);

        flyway.migrate();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM chart_special_flags", Integer.class))
                .isEqualTo(82);
        assertThat(jdbc.queryForObject(
                "SELECT has_strict_judgement FROM chart_special_flags WHERE chart_id = 29", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT has_strict_gauge FROM chart_special_flags WHERE chart_id = 783", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT extra_type FROM chart_special_flags WHERE chart_id = 6865", String.class))
                .isEqualTo("EXTRA");
        assertThat(jdbc.queryForObject(
                "SELECT extra_type FROM chart_special_flags WHERE chart_id = 7133", String.class))
                .isEqualTo("SUPER_EXTRA");
        assertThat(jdbc.queryForObject(
                "SELECT has_strict_gauge FROM chart_special_flags WHERE chart_id = 7133", Boolean.class))
                .isTrue();

        assertThat(jdbc.queryForObject(
                "SELECT has_strict_judgement FROM charts WHERE chart_id = 29", Boolean.class))
                .isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT has_strict_gauge FROM charts WHERE chart_id = 783", Boolean.class))
                .isFalse();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'songs'
                  AND column_name = 'extra_type'
                """, Integer.class)).isZero();
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
