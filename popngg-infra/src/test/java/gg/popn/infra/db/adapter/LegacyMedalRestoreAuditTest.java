package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMedalRestoreAuditTest extends MySqlIntegrationTestSupport {

    @Test
    void reportsOnlyUnchangedDormantRecordsAsReady() throws Exception {
        var source = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(source)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP TABLE IF EXISTS legacy_medal_audit_test_playdata");
        jdbc.execute("""
                CREATE TABLE legacy_medal_audit_test_playdata (
                    playdata_id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL,
                    chart_id BIGINT NOT NULL, score INT NOT NULL, medal INT NOT NULL)
                """);
        jdbc.execute("CREATE TABLE migration_playdata_map (old_playdata_id BIGINT PRIMARY KEY, new_playdata_id BIGINT NOT NULL)");
        jdbc.update("""
                INSERT INTO users (user_id,poptomo_id,password_hash,role,created_at,updated_at)
                VALUES (1,'0000-0000-0001','x','USER',NOW(),NOW()),
                       (2,'0000-0000-0002','x','USER',NOW(),NOW()),
                       (3,'0000-0000-0003','x','USER',NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO user_profiles (user_id,user_name,potential_popclass,created_at,updated_at)
                VALUES (1,'dormant',123,NOW(),NOW()),
                       (2,'renewed',456,NOW(),NOW()),
                       (3,'score-changed',789,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO songs (song_id,song_hash,genre_name,song_name,version,created_at,updated_at)
                VALUES (1,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa','g','s',28,NOW(),NOW())
                """);
        for (int chartId = 1; chartId <= 4; chartId++) {
            jdbc.update("""
                    INSERT INTO charts (chart_id,song_id,difficulty_code,difficulty_label,
                                        level,chart_version,is_deleted,created_at,updated_at)
                    VALUES (?,1,?,'EX',49,28,FALSE,NOW(),NOW())
                    """, chartId, chartId);
        }
        jdbc.update("""
                INSERT INTO legacy_medal_audit_test_playdata VALUES
                    (1,1,1,90000,11), (2,1,2,72000,12),
                    (3,2,3,80000,10), (4,3,4,85000,9)
                """);
        jdbc.update("""
                INSERT INTO playdata
                    (playdata_id,user_id,chart_id,current_version,version_score_known,
                     all_time_score,all_time_score_version,medal_code,created_at,updated_at)
                VALUES (1,1,1,29,FALSE,90000,28,9,NOW(),NOW()),
                       (2,1,2,29,FALSE,72000,28,12,NOW(),NOW()),
                       (3,2,3,29,FALSE,80000,28,8,NOW(),NOW()),
                       (4,3,4,29,FALSE,84000,28,11,NOW(),NOW())
                """);
        jdbc.update("INSERT INTO migration_playdata_map VALUES (1,1),(2,2),(3,3),(4,4)");
        jdbc.update("""
                INSERT INTO renew_logs (user_id,poptomo_id,status,mode,created_at)
                VALUES (2,'0000-0000-0002','SUCCESS','ALL','2026-08-29 15:00:00')
                """);

        Path workspace = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(workspace.resolve("migration/sql/05_audit_medals_for_restore.sql"))) {
            workspace = workspace.getParent();
        }
        String sql = (Files.readString(workspace.resolve(
                        "migration/sql/05_stage_medals_for_restore.sql"))
                + Files.readString(workspace.resolve(
                        "migration/sql/05_audit_medals_for_restore.sql")))
                .replace("__LEGACY_PLAYDATA__", "popngg_integration.legacy_medal_audit_test_playdata")
                .replace("__TARGET_DB__", "popngg_integration");
        try (Connection connection = source.getConnection();
             Statement statement = connection.createStatement()) {
            for (String command : sql.replaceAll("(?m)^--.*$", "").split(";")) {
                if (!command.isBlank()) statement.execute(command);
            }
            try (var unchanged = statement.executeQuery(
                    "SELECT medal_code FROM playdata ORDER BY playdata_id")) {
                var medals = new java.util.ArrayList<Integer>();
                while (unchanged.next()) medals.add(unchanged.getInt(1));
                assertThat(medals).containsExactly(9, 12, 8, 11);
            }
            // The audit drops its temporary table after reporting. Re-run
            // only the staging statements to inspect the classified rows.
            for (String command : sql.replaceAll("(?m)^--.*$", "").split(";")) {
                if (command.contains("SELECT audit_status") || command.contains("SELECT 'summary'")
                        || command.contains("DROP TEMPORARY TABLE")) break;
                if (!command.isBlank()) statement.execute(command);
            }
            try (var results = statement.executeQuery("""
                    SELECT audit_status, COUNT(*) FROM medal_restore_audit
                    GROUP BY audit_status ORDER BY audit_status
                    """)) {
                var statuses = new java.util.HashMap<String, Integer>();
                while (results.next()) statuses.put(results.getString(1), results.getInt(2));
                assertThat(statuses).containsEntry("READY", 1)
                        .containsEntry("SOURCE_NONE_REVIEW", 1)
                        .containsEntry("RENEWED_AFTER_AUG30", 1)
                        .containsEntry("SCORE_CHANGED", 1);
            }
            String applyTemplate = Files.readString(workspace.resolve(
                    "migration/sql/06_apply_legacy_medal_restore.sql"))
                    .replace("__TARGET_DB__", "popngg_integration");
            boolean wrongCountRejected = false;
            try {
                runSql(statement, applyTemplate.replace("__EXPECTED_READY__", "2"));
            } catch (SQLException expected) {
                wrongCountRejected = true;
            }
            assertThat(wrongCountRejected).isTrue();
            assertThat(jdbc.queryForObject(
                    "SELECT medal_code FROM playdata WHERE playdata_id=1", Integer.class))
                    .isEqualTo(9);
            statement.execute("DROP TEMPORARY TABLE medal_restore_assertion");
            statement.execute("DROP TEMPORARY TABLE medal_restore_users");
            statement.execute("DROP TEMPORARY TABLE medal_restore_ready");

            String apply = applyTemplate.replace("__EXPECTED_READY__", "1");
            runSql(statement, apply);
            try (var restored = statement.executeQuery(
                    "SELECT medal_code FROM playdata ORDER BY playdata_id")) {
                var medals = new java.util.ArrayList<Integer>();
                while (restored.next()) medals.add(restored.getInt(1));
                assertThat(medals).containsExactly(10, 12, 8, 11);
            }
            assertThat(jdbc.queryForObject(
                    "SELECT potential_popclass FROM user_profiles WHERE user_id=2", Integer.class))
                    .isEqualTo(456);
        }
    }

    private static void runSql(Statement statement, String sql) throws SQLException {
        for (String command : sql.replaceAll("(?m)^--.*$", "").split(";")) {
            if (!command.isBlank()) statement.execute(command);
        }
    }
}
