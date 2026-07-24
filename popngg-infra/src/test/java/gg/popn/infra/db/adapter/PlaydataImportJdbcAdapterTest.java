package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.service.PlaydataUpsertPolicy;
import gg.popn.application.playdata.service.PlaydataHistoryPolicy;
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
        jdbc.execute("ALTER TABLE user_profiles ADD display_popclass INT DEFAULT 0");
        jdbc.execute("ALTER TABLE user_profiles ADD potential_popclass INT DEFAULT 0");
        jdbc.execute("ALTER TABLE user_profiles ADD legacy_popclass INT DEFAULT 0");
        jdbc.execute("""
                CREATE TABLE songs(song_id BIGINT PRIMARY KEY, song_hash VARCHAR(32),
                  song_name VARCHAR(255), genre_name VARCHAR(255))
                """);
        jdbc.execute("""
                CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, song_id BIGINT,
                  difficulty_code INT, level INT, chart_version INT,
                  is_upper BOOLEAN, is_deleted BOOLEAN)
                """);
        jdbc.execute("""
                CREATE TABLE renew_logs(renew_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  poptomo_id VARCHAR(32), user_id BIGINT, status VARCHAR(20), mode VARCHAR(20),
                  input_chart_count INT, matched_chart_count INT, updated_playdata_count INT,
                  failure_reason VARCHAR(1024), ip VARCHAR(45), created_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE playdata(playdata_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT, chart_id BIGINT, current_version INT, version_score INT,
                  version_rank_code INT, all_time_score INT, all_time_score_version INT,
                  all_time_rank_code INT, medal_code INT, popclass INT,
                  is_display_popclass_target BOOLEAN, popclass_bucket VARCHAR(20),
                  popclass_bucket_rank INT, last_renew_log_id BIGINT,
                  created_at TIMESTAMP, updated_at TIMESTAMP,
                  UNIQUE(user_id, chart_id))
                """);
        jdbc.execute("""
                CREATE TABLE game_version_transitions(transition_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  from_version INT, to_version INT, score_policy VARCHAR(20), status VARCHAR(20))
                """);
        jdbc.execute("""
                CREATE TABLE playdata_history(history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT, chart_id BIGINT, game_version INT,
                  previous_version_score INT, version_score INT,
                  previous_all_time_score INT, all_time_score INT,
                  previous_rank_code INT, rank_code INT,
                  previous_medal_code INT, medal_code INT, popclass INT,
                  event_type VARCHAR(32), renew_log_id BIGINT, created_at TIMESTAMP)
                """);
        jdbc.update("INSERT INTO users VALUES (1, '0000-0000-0000')");
        jdbc.update("""
                INSERT INTO user_profiles
                    (user_id, user_name, character_name, normal_credit, extra_credit,
                     time_play_10_credit, time_play_16_credit, updated_at)
                VALUES (1, 'old', '', 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO songs VALUES (10, 'hash', 'song', 'genre')");
        jdbc.update("INSERT INTO charts VALUES (100, 10, 3, 48, 29, FALSE, FALSE)");
        adapter = new PlaydataImportJdbcAdapter(
                jdbc, new PlaydataUpsertPolicy(), new PlaydataHistoryPolicy(),
                new gg.popn.application.playdata.service.PopclassPolicy(), 29);
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
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM playdata", Integer.class))
                .isEqualTo(1);
        assertThat(result.historyCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT event_type FROM playdata_history", String.class))
                .isEqualTo("REGISTER");
        assertThat(jdbc.queryForObject("SELECT status FROM renew_logs", String.class))
                .isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("SELECT normal_credit FROM user_profiles", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void reportsNotFoundAndAmbiguousRowsWithoutSensitiveValues() {
        jdbc.update("INSERT INTO songs VALUES (11, 'hash', 'song', 'genre')");
        jdbc.update("INSERT INTO charts VALUES (101, 11, 3, 48, 29, FALSE, FALSE)");
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
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.historyCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM renew_logs", String.class))
                .isEqualTo("PARTIAL_SUCCESS");
    }

    @Test
    void updatesHigherScoresRanksMedalAndRenewLogWithoutDuplicateRows() {
        adapter.execute(command(rowWithValues(100L, 90_000, 4, 2)));

        var result = adapter.execute(command(rowWithValues(100L, 95_000, 3, 5)));

        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.historyCount()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM playdata", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT current_version, version_score, version_rank_code,
                       all_time_score, all_time_score_version, all_time_rank_code, medal_code
                  FROM playdata
                """)).containsEntry("current_version", 29)
                .containsEntry("version_score", 95_000)
                .containsEntry("version_rank_code", 3)
                .containsEntry("all_time_score", 95_000)
                .containsEntry("all_time_score_version", 29)
                .containsEntry("all_time_rank_code", 3)
                .containsEntry("medal_code", 5);
        assertThat(jdbc.queryForObject("SELECT last_renew_log_id FROM playdata", Long.class))
                .isEqualTo(result.renewLogId());
    }

    @Test
    void leavesScoresAndRanksUntouchedForLowerObservationButUpdatesMedal() {
        adapter.execute(command(rowWithValues(100L, 95_000, 3, 2)));

        var result = adapter.execute(command(rowWithValues(100L, 90_000, 1, 6)));

        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.historyCount()).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT version_score, version_rank_code, all_time_score,
                       all_time_rank_code, medal_code FROM playdata
                """)).containsEntry("version_score", 95_000)
                .containsEntry("version_rank_code", 3)
                .containsEntry("all_time_score", 95_000)
                .containsEntry("all_time_rank_code", 3)
                .containsEntry("medal_code", 6);
    }

    @Test
    void returnsZeroUpdatesForIdenticalObservation() {
        var row = rowWithValues(100L, 90_000, 2, 3);
        adapter.execute(command(row));

        var result = adapter.execute(command(row));
        assertThat(result.updatedCount()).isZero();
        assertThat(result.historyCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM playdata_history", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void requiresApprovedVersionTransition() {
        insertVersion28State();

        assertThatThrownBy(() -> adapter.execute(command(
                rowWithValues(100L, 90_000, 4, 5))))
                .isInstanceOf(PlaydataUpsertPolicy.MissingGameVersionTransitionException.class);
        assertThat(jdbc.queryForObject("SELECT current_version FROM playdata", Integer.class))
                .isEqualTo(28);
    }

    @Test
    void appliesApprovedResetTransition() {
        insertVersion28State();
        jdbc.update("""
                INSERT INTO game_version_transitions(from_version, to_version, score_policy, status)
                VALUES (28, 29, 'RESET', 'APPROVED')
                """);

        var result = adapter.execute(command(rowWithValues(100L, 90_000, 4, 5)));

        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.historyCount()).isEqualTo(3);
        assertThat(jdbc.query("""
                SELECT event_type FROM playdata_history ORDER BY history_id
                """, (rs, rowNum) -> rs.getString(1))).containsExactly(
                "VERSION_INITIALIZED", "RANK_CHANGED", "MEDAL_CHANGED");
        assertThat(jdbc.queryForMap("""
                SELECT current_version, version_score, all_time_score,
                       all_time_score_version, medal_code FROM playdata
                """)).containsEntry("current_version", 29)
                .containsEntry("version_score", 90_000)
                .containsEntry("all_time_score", 95_000)
                .containsEntry("all_time_score_version", 28)
                .containsEntry("medal_code", 5);
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

    private static ImportPlaydataCommand command(ImportPlaydataCommand.Row row) {
        return new ImportPlaydataCommand("0000-0000-0000", null, List.of(row));
    }

    private static ImportPlaydataCommand.Row rowWithValues(long chartId, int score,
                                                            int rank, int medal) {
        return new ImportPlaydataCommand.Row(chartId, null, null, null,
                null, null, null, score, rank, medal);
    }

    private void insertVersion28State() {
        jdbc.update("""
                INSERT INTO playdata
                    (user_id, chart_id, current_version, version_score, version_rank_code,
                     all_time_score, all_time_score_version, all_time_rank_code, medal_code,
                     popclass, is_display_popclass_target, last_renew_log_id, created_at, updated_at)
                VALUES (1, 100, 28, 95000, 2, 95000, 28, 2, 3,
                        0, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
    }
}
