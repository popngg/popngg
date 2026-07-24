package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaydataImportJdbcAdapterTest {
    private JdbcTemplate jdbc;
    private PlaydataImportJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:import;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE users(user_id BIGINT PRIMARY KEY, poptomo_id VARCHAR(32) UNIQUE)");
        jdbc.execute("""
                CREATE TABLE user_profiles(user_id BIGINT PRIMARY KEY, user_name VARCHAR(64),
                  character_name VARCHAR(128), normal_credit INT, extra_credit INT,
                  time_play_10_credit INT, time_play_16_credit INT, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE songs(song_id BIGINT PRIMARY KEY, song_hash VARCHAR(32),
                  song_name VARCHAR(255), genre_name VARCHAR(255))
                """);
        jdbc.execute("""
                CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, song_id BIGINT,
                  difficulty_code INT, is_upper BOOLEAN, is_deleted BOOLEAN)
                """);
        jdbc.execute("""
                CREATE TABLE renew_logs(renew_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  poptomo_id VARCHAR(32), user_id BIGINT, status VARCHAR(20), mode VARCHAR(20),
                  input_chart_count INT, matched_chart_count INT, updated_playdata_count INT,
                  failure_reason VARCHAR(1024), ip VARCHAR(45), created_at TIMESTAMP)
                """);
        jdbc.update("INSERT INTO users VALUES (1, '0000-0000-0000')");
        jdbc.update("INSERT INTO user_profiles VALUES (1, 'old', '', 0, 0, 0, 0, CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO songs VALUES (10, 'hash', 'song', 'genre')");
        jdbc.update("INSERT INTO charts VALUES (100, 10, 3, FALSE, FALSE)");
        adapter = new PlaydataImportJdbcAdapter(jdbc);
    }

    @Test
    void matchesAllSupportedIdentitiesAndUpdatesProfileSnapshot() {
        var profile = new ImportPlaydataCommand.ProfileSnapshot("new", "character", 1, 2, 3, 4);
        var rows = List.of(
                row(100L, null, null, null, null, null, null),
                row(null, 10L, 3, false, null, null, null),
                row(null, null, 3, false, "hash", null, null),
                row(null, null, 3, false, null, "song", "genre"));

        var result = adapter.execute(new ImportPlaydataCommand("0000-0000-0000", profile, rows));

        assertThat(result.receivedCount()).isEqualTo(4);
        assertThat(result.matchedCount()).isEqualTo(4);
        assertThat(result.skippedCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM renew_logs", String.class))
                .isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("SELECT normal_credit FROM user_profiles", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void reportsNotFoundAndAmbiguousRowsWithoutSensitiveValues() {
        jdbc.update("INSERT INTO songs VALUES (11, 'hash', 'song', 'genre')");
        jdbc.update("INSERT INTO charts VALUES (101, 11, 3, FALSE, FALSE)");
        var rows = List.of(
                row(999L, null, null, null, null, null, null),
                row(null, null, 3, false, "hash", null, null));

        var result = adapter.execute(new ImportPlaydataCommand("0000-0000-0000", null, rows));

        assertThat(result.matchedCount()).isZero();
        assertThat(result.unmatched()).extracting(ImportPlaydataResult.UnmatchedRow::reason)
                .isNotEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM renew_logs", String.class))
                .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT failure_reason FROM renew_logs", String.class))
                .isEqualTo("CHART_NOT_FOUND=1,AMBIGUOUS_CHART=1");
    }

    @Test
    void marksMixedMatchesAsPartialSuccess() {
        var rows = List.of(
                row(100L, null, null, null, null, null, null),
                row(999L, null, null, null, null, null, null));

        var result = adapter.execute(new ImportPlaydataCommand("0000-0000-0000", null, rows));

        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM renew_logs", String.class))
                .isEqualTo("PARTIAL_SUCCESS");
    }

    @Test
    void rejectsUnknownAuthenticatedUserBeforeCreatingLog() {
        assertThatThrownBy(() -> adapter.execute(
                new ImportPlaydataCommand("missing", null,
                        List.of(row(100L, null, null, null, null, null, null)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM renew_logs", Integer.class)).isZero();
    }

    private static ImportPlaydataCommand.Row row(Long chartId, Long songId, Integer difficulty,
                                                  Boolean upper, String hash, String song,
                                                  String genre) {
        return new ImportPlaydataCommand.Row(chartId, songId, difficulty, upper, hash,
                song, genre, 90_000, 2, 3);
    }
}
