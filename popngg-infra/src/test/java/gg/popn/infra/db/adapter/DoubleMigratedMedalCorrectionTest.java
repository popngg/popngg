package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleMigratedMedalCorrectionTest extends MySqlIntegrationTestSupport {

    @Test
    void reversesOneConversionOnlyForUnrenewedLegacyRows() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("28").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO users (user_id,poptomo_id,password_hash,role,created_at,updated_at)
                VALUES (1,'0000-0000-0001','x','USER',NOW(),NOW()),
                       (2,'2987-4104-7041','x','USER',NOW(),NOW()),
                       (3,'9003-3008-7558','x','USER',NOW(),NOW()),
                       (4,'0000-0000-0004','x','USER',NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO user_profiles
                  (user_id,user_name,character_name,comment,is_hidden,display_popclass,
                   potential_popclass,legacy_popclass,created_at,updated_at)
                VALUES (1,'legacy','','',FALSE,0,456,0,NOW(),NOW()),
                       (2,'legacy2','','',FALSE,0,456,0,NOW(),NOW()),
                       (3,'legacy3','','',FALSE,0,456,0,NOW(),NOW()),
                       (4,'renewed','','',FALSE,0,456,0,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO songs
                  (song_id,song_hash,genre_name,song_name,version,created_at,updated_at)
                VALUES (1,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa','g','s',28,NOW(),NOW())
                """);
        for (int id = 1; id <= 5; id++) {
            jdbc.update("""
                    INSERT INTO charts
                      (chart_id,song_id,difficulty_code,difficulty_label,level,
                       chart_version,is_deleted,created_at,updated_at)
                    VALUES (?,1,?, ?,49,28,FALSE,NOW(),NOW())
                    """, id, id, "D" + id);
        }
        jdbc.update("""
                INSERT INTO charts
                  (chart_id,song_id,difficulty_code,difficulty_label,level,
                   chart_version,is_deleted,created_at,updated_at)
                VALUES (2525,1,4,'EX',49,21,FALSE,NOW(),NOW()),
                       (677,1,4,'EX',46,12,FALSE,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO playdata
                  (playdata_id,user_id,chart_id,current_version,version_score,
                   version_score_known,all_time_score,all_time_score_version,
                   medal_code,popclass,potential_popclass,last_renew_log_id,
                   created_at,updated_at)
                VALUES
                  (1,1,1,29,0,FALSE,90000,28,10,0,123,NULL,NOW(),NOW()),
                  (2,1,2,29,0,FALSE,90000,28,11,0,123,NULL,NOW(),NOW()),
                  (3,1,3,29,0,FALSE,90000,28,8,0,123,NULL,NOW(),NOW()),
                  (4,1,4,29,0,FALSE,90000,28,9,0,123,NULL,NOW(),NOW()),
                  (5,1,5,29,90000,TRUE,90000,29,9,123,123,99,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO playdata
                  (playdata_id,user_id,chart_id,current_version,version_score,
                   version_score_known,all_time_score,all_time_score_version,
                   all_time_rank_code,medal_code,popclass,potential_popclass,
                   last_renew_log_id,created_at,updated_at)
                VALUES (6,2,2525,29,0,FALSE,72430,28,9,12,0,123,NULL,NOW(),NOW()),
                       (7,3,677,29,0,TRUE,0,29,13,7,0,0,99,NOW(),NOW()),
                       (8,4,1,29,0,FALSE,90000,28,9,8,0,123,NULL,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO renew_logs
                  (poptomo_id,user_id,status,mode,input_chart_count,
                   matched_chart_count,updated_playdata_count,created_at)
                VALUES ('9003-3008-7558',3,'SUCCESS','IMPORT',1,1,1,'2026-09-03 10:00:00'),
                       ('0000-0000-0004',4,'SUCCESS','IMPORT',1,1,1,'2026-08-30 00:00:00')
                """);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .load().migrate();

        assertThat(jdbc.queryForList(
                "SELECT medal_code FROM playdata ORDER BY playdata_id", Integer.class))
                .containsExactly(11, 8, 9, 10, 9, 10, 7, 8);
        assertThat(jdbc.queryForObject(
                "SELECT potential_popclass FROM user_profiles WHERE user_id=1", Integer.class))
                .isNotEqualTo(456);
        assertThat(jdbc.queryForObject(
                "SELECT revision FROM user_directory_revision WHERE id=1", Long.class))
                .isEqualTo(2L);
    }
}
