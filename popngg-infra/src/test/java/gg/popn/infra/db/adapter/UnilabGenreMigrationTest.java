package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

class UnilabGenreMigrationTest extends MySqlIntegrationTestSupport {
    @Test
    void refreshesGenreWithoutChangingIdentityOrOverwritingNewerMetadata() {
        var dataSource = mysqlDataSource();
        var flyway = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("21").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO songs (song_id,song_hash,song_name,genre_name,artist_name,version,
                                   jacket_url,created_at,updated_at)
                VALUES (1540,'existing-hash','Airplane','Airplane','Red Planets',27,
                        'https://static.popn.gg/existing.png',NOW(),NOW()),
                       (1539,'newer-edit','001 -どうしんのかいろ-','newer genre','90°club',27,
                        NULL,NOW(),NOW()),
                       (1541,'other-artist','Awakening Wings','Awakening Wings','other artist',27,
                        NULL,NOW(),NOW())
                """);
        jdbc.update("""
                INSERT INTO charts (chart_id,song_id,difficulty_code,difficulty_label,level,
                                    chart_version,is_upper,created_at,updated_at)
                VALUES (5367,1540,4,'EX',45,27,FALSE,NOW(),NOW())
                """);
        var charts = jdbc.queryForList("SELECT * FROM charts");
        var preserved = jdbc.queryForMap("""
                SELECT song_id,song_hash,song_name,artist_name,version,jacket_url,created_at
                  FROM songs WHERE song_id=1540
                """);
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT genre_name FROM songs WHERE song_id=1540", String.class))
                .isEqualTo("ワールドオルタナティブ");
        assertThat(jdbc.queryForMap("""
                SELECT song_id,song_hash,song_name,artist_name,version,jacket_url,created_at
                  FROM songs WHERE song_id=1540
                """)).isEqualTo(preserved);
        assertThat(jdbc.queryForList("SELECT * FROM charts")).isEqualTo(charts);
        assertThat(jdbc.queryForObject("SELECT genre_name FROM songs WHERE song_id=1539", String.class))
                .isEqualTo("newer genre");
        assertThat(jdbc.queryForObject("SELECT genre_name FROM songs WHERE song_id=1541", String.class))
                .isEqualTo("Awakening Wings");
        // Execute the SQL again, beyond Flyway's own applied-version protection.
        var after = jdbc.queryForList("SELECT * FROM songs ORDER BY song_id");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V22__refresh_unilab_genres.sql"))
                .execute(dataSource);
        assertThat(jdbc.queryForList("SELECT * FROM songs ORDER BY song_id")).isEqualTo(after);
    }
}
