package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class SyntheticBotCleanupMySqlTest extends MySqlIntegrationTestSupport {
    @Test void removesOnlySyntheticBotsAndTheirRecords() {
        var ds=mysqlDataSource();
        var baseline=Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("24").cleanDisabled(false).load();
        baseline.clean();baseline.migrate();
        var jdbc=new JdbcTemplate(ds);
        jdbc.update("""
                INSERT INTO users(user_id,poptomo_id,password_hash,role,created_at,updated_at) VALUES
                (1,'BOT-1-1','test','BOT',NOW(),NOW()),
                (2,'0000-0000-0002','test','USER',NOW(),NOW()),
                (3,'BOT-2-2','test','USER',NOW(),NOW()),
                (4,'0000-0000-0004','test','BOT',NOW(),NOW())
                """);
        jdbc.update("INSERT INTO user_profiles(user_id,user_name,created_at,updated_at) SELECT user_id,'test',NOW(),NOW() FROM users");
        jdbc.update("INSERT INTO songs(song_id,genre_name,song_name,version,created_at,updated_at) VALUES(1,'test','test',29,NOW(),NOW())");
        jdbc.update("INSERT INTO charts(chart_id,song_id,difficulty_code,difficulty_label,level,chart_version,created_at,updated_at) VALUES(1,1,3,'EX',49,29,NOW(),NOW())");
        jdbc.update("INSERT INTO playdata(user_id,chart_id,current_version,all_time_score,medal_code,created_at,updated_at) SELECT user_id,1,29,95000,7,NOW(),NOW() FROM users");
        jdbc.update("INSERT INTO user_clear_levels(user_id,current_version,clear_level) SELECT user_id,29,49 FROM users");
        jdbc.update("INSERT INTO playdata_history(user_id,chart_id,game_version,event_type,created_at) SELECT user_id,1,29,'REGISTER',NOW() FROM users");
        jdbc.update("INSERT INTO login_logs(user_id,poptomo_id,status,created_at) SELECT user_id,poptomo_id,'SUCCESS',NOW() FROM users");
        jdbc.update("INSERT INTO renew_logs(user_id,poptomo_id,status,mode,created_at) SELECT user_id,poptomo_id,'SUCCESS','ALL',NOW() FROM users");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        assertThat(jdbc.queryForList("SELECT user_id FROM users ORDER BY user_id",Long.class)).containsExactly(2L,3L,4L);
        for(String table:java.util.List.of("user_profiles","playdata","playdata_history","user_clear_levels","login_logs","renew_logs"))
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE user_id=1",Integer.class)).as(table).isZero();
        assertThat(jdbc.queryForObject("SELECT revision FROM user_directory_revision WHERE id=1",Long.class)).isEqualTo(2L);
    }
}
