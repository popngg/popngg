package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.assertj.core.api.Assertions.assertThat;

class OctoberCatalogMigrationTest extends MySqlIntegrationTestSupport {
    @Test
    void correctsReviewedMetadataAndLevelWhilePreservingIdsAndNewerEdits() {
        var ds = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(ds).locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("28").load().migrate();
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("""
                INSERT INTO songs (song_id,song_hash,song_name,genre_name,artist_name,version,jacket_url,created_at,updated_at)
                VALUES (1891,'preserved-1','Bomb be love','Bomb be love','sei☆shin feat.ken',28,'https://static.popn.gg/old.png',NOW(),NOW()),
                       (1892,'preserved-2','BROADWAY QUEENS','administrator genre','T-HEY & TANEKO',28,NULL,NOW(),NOW()),
                       (1912,'preserved-3','Saturday''s Reward','Saturday''s Reward',NULL,28,NULL,NOW(),NOW()),
                       (2131,'preserved-4','TEKNO KEISER','デステクノ','STEVIE(Trymatic)/Nana Takahashi Prod. by L.E.D.',29,NULL,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO charts (chart_id,song_id,difficulty_code,difficulty_label,level,chart_version,is_upper,created_at,updated_at)
                VALUES (7548,2131,4,'EX',47,29,FALSE,NOW(),NOW()), (7547,2131,3,'HYPER',41,29,FALSE,NOW(),NOW())
                """);
        var identities = jdbc.queryForList("SELECT song_id,song_hash,song_name,version,jacket_url,created_at FROM songs ORDER BY song_id");
        var hyper = jdbc.queryForMap("SELECT * FROM charts WHERE chart_id=7547");
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT genre_name FROM songs WHERE song_id=1891",String.class)).isEqualTo("エヴォリューションディスコ");
        assertThat(jdbc.queryForObject("SELECT genre_name FROM songs WHERE song_id=1892",String.class)).isEqualTo("administrator genre");
        assertThat(jdbc.queryForObject("SELECT genre_name FROM songs WHERE song_id=1912",String.class)).isEqualTo("ホリデーラウンジ");
        assertThat(jdbc.queryForObject("SELECT artist_name FROM songs WHERE song_id=1912",String.class)).isEqualTo("BEMANI Sound Team \"劇団レコード\" feat.秋成");
        assertThat(jdbc.queryForObject("SELECT level FROM charts WHERE chart_id=7548",Integer.class)).isEqualTo(46);
        assertThat(jdbc.queryForMap("SELECT * FROM charts WHERE chart_id=7547")).isEqualTo(hyper);
        assertThat(jdbc.queryForList("SELECT song_id,song_hash,song_name,version,jacket_url,created_at FROM songs ORDER BY song_id")).isEqualTo(identities);
        var after = jdbc.queryForList("SELECT * FROM songs ORDER BY song_id");
        var resource = new ClassPathResource("db/migration/V29__refresh_jam_fizz_genres_and_high_cheers_levels.sql");
        new ResourceDatabasePopulator(resource).execute(ds);
        assertThat(jdbc.queryForList("SELECT * FROM songs ORDER BY song_id")).isEqualTo(after);
        jdbc.update("UPDATE charts SET level=48 WHERE chart_id=7548");
        new ResourceDatabasePopulator(resource).execute(ds);
        assertThat(jdbc.queryForObject("SELECT level FROM charts WHERE chart_id=7548",Integer.class)).isEqualTo(48);
    }
}
