package gg.popn.infra.db.adapter;

import gg.popn.application.tier.AchievementBoard;
import gg.popn.application.tier.AchievementBoardService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.assertj.core.api.Assertions.*;

class AchievementBoardJdbcAdapterTest {
    @Test void countsUniquePublicUsersAtScoreBoundaryAndPreservesZeroCharts() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:achievements;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        var scripts = new ResourceDatabasePopulator();
        for (String file : new String[]{"V1__baseline_account_and_security.sql", "V2__baseline_music_catalog.sql", "V3__baseline_playdata_and_history.sql"})
            scripts.addScript(new ClassPathResource("db/migration/" + file));
        scripts.execute(ds);
        var jdbc = new JdbcTemplate(ds);
        for (int chart : new int[]{1,51,52}) {
            jdbc.update("INSERT INTO songs(song_id,song_hash,song_name,genre_name,version,created_at,updated_at) VALUES (?,?,'Test','Genre',29,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", chart, "hash"+chart);
            jdbc.update("INSERT INTO charts(chart_id,song_id,difficulty_code,difficulty_label,level,chart_version,is_deleted,created_at,updated_at) VALUES (?,?,4,'EX',40,29,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", chart, chart, chart==52);
        }
        for (int user=1; user<=9; user++) {
            jdbc.update("INSERT INTO users(user_id,poptomo_id,password_hash,role,created_at,updated_at) VALUES (?,?,'unused',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", user, "0000-0000-000"+user, user==6 ? "BOT" : "USER");
            jdbc.update("INSERT INTO user_profiles(user_id,user_name,is_hidden,created_at,updated_at) VALUES (?,'Player',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", user, user==5);
            jdbc.update("INSERT INTO playdata(user_id,chart_id,current_version,all_time_score,medal_code,created_at,updated_at) VALUES (?,1,?,90000,7,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", user, user==8 ? 28 : 29);
        }
        jdbc.update("UPDATE charts SET level=40 WHERE chart_id IN (1,51,52)");
        jdbc.update("UPDATE playdata SET all_time_score=98000,medal_code=1 WHERE chart_id=1 AND user_id IN (1,5,6)");
        jdbc.update("UPDATE playdata SET all_time_score=97999 WHERE chart_id=1 AND user_id=2");
        jdbc.update("UPDATE playdata SET all_time_score=100001 WHERE chart_id=1 AND user_id=3");
        jdbc.update("UPDATE playdata SET all_time_score=100000 WHERE chart_id=1 AND user_id=8");
        jdbc.update("UPDATE playdata SET medal_code=1 WHERE chart_id=1 AND user_id=4");
        var service = new AchievementBoardService(new AchievementBoardJdbcAdapter(jdbc), 29);
        var board = service.load(40, AchievementBoard.Metric.S);
        assertThat(board.charts()).extracting(AchievementBoard.Chart::chartId).containsExactly(1L, 51L);
        assertThat(board.charts().getFirst().sCount()).isEqualTo(2); // includes old-version best, excludes bot/hidden/invalid score
        assertThat(board.charts().getFirst().perfectCount()).isEqualTo(2); // medal is independent of score
        assertThat(board.charts().getLast().sCount()).isZero();
        jdbc.update("UPDATE charts SET chart_version=30 WHERE chart_id=51");
        assertThat(service.load(40, AchievementBoard.Metric.PERFECT).charts()).hasSize(1);
        assertThatThrownBy(() -> service.load(39, AchievementBoard.Metric.S)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.load(51, AchievementBoard.Metric.S)).isInstanceOf(IllegalArgumentException.class);
    }
}
