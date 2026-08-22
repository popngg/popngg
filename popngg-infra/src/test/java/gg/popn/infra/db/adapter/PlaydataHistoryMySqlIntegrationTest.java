package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.service.PlaydataHistoryPolicy;
import gg.popn.application.playdata.service.PlaydataUpsertPolicy;
import gg.popn.application.playdata.service.PopclassPolicy;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class PlaydataHistoryMySqlIntegrationTest extends MySqlIntegrationTestSupport {
    private JdbcTemplate jdbc;
    private PlaydataImportJdbcAdapter adapter;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        var dataSource = mysqlDataSource();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(dataSource);
        adapter = new PlaydataImportJdbcAdapter(
                jdbc, new PlaydataUpsertPolicy(), new PlaydataHistoryPolicy(),
                new PopclassPolicy(), 29);
        PlatformTransactionManager manager = new DataSourceTransactionManager(dataSource);
        transaction = new TransactionTemplate(manager);
        seed();
    }

    @Test
    void appliesFlywayAndAppendsHistoryInSameMySqlTransaction() {
        var first = transaction.execute(status -> adapter.execute(command(90_000, 4, 2)));
        var second = transaction.execute(status -> adapter.execute(command(95_000, 3, 5)));

        assertThat(first).isNotNull();
        assertThat(first.historyCount()).isEqualTo(1);
        assertThat(second).isNotNull();
        assertThat(second.historyCount()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM playdata", Integer.class)).isEqualTo(1);
        assertThat(jdbc.query("""
                SELECT event_type FROM playdata_history ORDER BY history_id
                """, (rs, rowNum) -> rs.getString(1))).containsExactly(
                "REGISTER", "SCORE_UP", "ALL_TIME_SCORE_UP", "RANK_CHANGED", "MEDAL_CHANGED");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM playdata_history
                 WHERE game_version = 29 AND renew_log_id IS NOT NULL
                """, Integer.class)).isEqualTo(5);
    }

    @Test
    void rollsBackStateAndHistoryTogether() {
        transaction.executeWithoutResult(status -> {
            adapter.execute(command(90_000, 4, 2));
            status.setRollbackOnly();
        });

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM playdata", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM playdata_history", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM renew_logs", Integer.class)).isZero();
    }

    @Test
    void rebuildsPopclassAndMarksTwentyCurrentAndFortyOldCharts() {
        jdbc.update("UPDATE user_profiles SET legacy_popclass = 7777 WHERE user_id = 1");
        var rows = new ArrayList<ImportPlaydataCommand.Row>();
        rows.add(row(100, 90_000, 2));
        for (int index = 1; index < 70; index++) {
            long chartId = 100L + index;
            int chartVersion = index <= 24 ? 29 : 28;
            boolean upper = index == 24;
            jdbc.update("""
                    INSERT INTO charts
                        (chart_id, song_id, difficulty_code, difficulty_label, level,
                         chart_version, is_upper, created_at, updated_at)
                    VALUES (?, 10, ?, ?, ?, ?, ?, NOW(), NOW())
                    """, chartId, 10 + index, "IT-" + index, 30 + index % 20,
                    chartVersion, upper);
            rows.add(row(chartId, 80_000 + index * 100, index % 8 + 1));
        }

        transaction.executeWithoutResult(status -> adapter.execute(
                new ImportPlaydataCommand("0000-0000-0000", null, rows)));

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM playdata
                 WHERE user_id = 1 AND popclass_bucket = 'CURRENT_VERSION'
                   AND is_display_popclass_target = TRUE
                """, Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM playdata
                 WHERE user_id = 1 AND popclass_bucket = 'OLD_VERSION'
                   AND is_display_popclass_target = TRUE
                """, Integer.class)).isEqualTo(40);
        assertThat(jdbc.queryForObject("""
                SELECT popclass_bucket FROM playdata WHERE chart_id = 124
                """, String.class)).isEqualTo("CURRENT_VERSION");
        assertThat(jdbc.queryForObject("""
                SELECT MAX(popclass_bucket_rank) FROM playdata
                 WHERE popclass_bucket = 'CURRENT_VERSION'
                """, Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject("""
                SELECT MAX(popclass_bucket_rank) FROM playdata
                 WHERE popclass_bucket = 'OLD_VERSION'
                """, Integer.class)).isEqualTo(40);
        assertThat(jdbc.queryForObject("""
                SELECT legacy_popclass FROM user_profiles WHERE user_id = 1
                """, Integer.class)).isPositive();
        assertThat(jdbc.queryForObject("""
                SELECT display_popclass > 0 AND potential_popclass > 0
                  FROM user_profiles WHERE user_id = 1
                """, Boolean.class)).isTrue();
    }

    @Test
    void recalculatesAndStoresLegacyAndNewPopclassesInMySql() {
        jdbc.update("UPDATE charts SET level = 49 WHERE chart_id = 100");
        jdbc.update("""
                INSERT INTO playdata
                    (user_id, chart_id, current_version, version_score, version_score_known,
                     version_rank_code,
                     all_time_score, all_time_score_version, all_time_rank_code, medal_code,
                     created_at, updated_at)
                VALUES (1, 100, 29, 80000, TRUE, 7, 90000, 29, 5, 4, NOW(), NOW())
                """);

        var result = transaction.execute(status -> adapter.recalculate("0000-0000-0000"));

        assertThat(result).isNotNull();
        assertThat(result.displayPopclass()).isEqualTo(2_919);
        assertThat(result.potentialPopclass()).isEqualTo(3_045);
        assertThat(result.legacyPopclass()).isEqualTo(196);
        assertThat(result.newPopclassScale()).isEqualTo(1_000);
        assertThat(jdbc.queryForMap("""
                SELECT display_popclass, potential_popclass, legacy_popclass
                  FROM user_profiles WHERE user_id = 1
                """))
                .containsEntry("display_popclass", 2_919)
                .containsEntry("potential_popclass", 3_045)
                .containsEntry("legacy_popclass", 196);
    }

    @Test
    void keepsLegacyAndPotentialValuesWhenCurrentVersionScoreWasReset() {
        jdbc.update("UPDATE charts SET level = 49 WHERE chart_id = 100");
        jdbc.update("""
                INSERT INTO playdata
                    (user_id, chart_id, current_version, version_score, version_rank_code,
                     all_time_score, all_time_score_version, all_time_rank_code, medal_code,
                     created_at, updated_at)
                VALUES (1, 100, 28, 90000, 5, 90000, 28, 5, 4, NOW(), NOW())
                """);

        var result = transaction.execute(status -> adapter.recalculate("0000-0000-0000"));

        assertThat(result).isNotNull();
        assertThat(result.displayPopclass()).isZero();
        assertThat(result.potentialPopclass()).isEqualTo(3_045);
        assertThat(result.legacyPopclass()).isEqualTo(196);
    }

    private void seed() {
        jdbc.update("""
                INSERT INTO users
                    (user_id, poptomo_id, password_hash, role, created_at, updated_at)
                VALUES (1, '0000-0000-0000', 'test-only', 'USER', NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO user_profiles
                    (user_id, user_name, character_name, comment, is_hidden,
                     created_at, updated_at)
                VALUES (1, 'integration', '', '', FALSE, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO songs
                    (song_id, song_hash, genre_name, song_name, version, created_at, updated_at)
                VALUES (10, 'integration-hash', 'genre', 'song', 29, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO charts
                    (chart_id, song_id, difficulty_code, difficulty_label, level,
                     chart_version, is_upper, created_at, updated_at)
                VALUES (100, 10, 3, 'EX', 48, 29, FALSE, NOW(), NOW())
                """);
    }

    private static ImportPlaydataCommand command(int score, int rank, int medal) {
        var row = new ImportPlaydataCommand.Row(100L, null, null, null,
                null, null, null, score, rank, medal, score, true, null);
        return new ImportPlaydataCommand("0000-0000-0000", null, List.of(row));
    }

    private static ImportPlaydataCommand.Row row(long chartId, int score, int medal) {
        return new ImportPlaydataCommand.Row(chartId, null, null, null,
                null, null, null, score, 4, medal, score, true, null);
    }
}
