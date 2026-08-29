package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.command.UpdateSongCommand;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateSongJdbcAdapterTest {
    @Test
    void updatesSongAndSelectedChart() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:update-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE songs(song_id BIGINT PRIMARY KEY,song_hash VARCHAR(64) UNIQUE,genre_name VARCHAR(255),song_name VARCHAR(255),artist_name VARCHAR(255),version INT,jacket_url VARCHAR(512),created_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE charts(chart_id BIGINT PRIMARY KEY,song_id BIGINT,level INT,chart_version INT,is_upper BOOLEAN,has_strict_gauge BOOLEAN,has_strict_judgement BOOLEAN,is_deleted BOOLEAN,updated_at TIMESTAMP)");
        jdbc.update("INSERT INTO songs VALUES(1,'old','g','s','a',1,NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO charts VALUES(10,1,30,1,FALSE,FALSE,FALSE,FALSE,CURRENT_TIMESTAMP)");
        var adapter = new UpdateSongJdbcAdapter(new NamedParameterJdbcTemplate(jdbc));
        var command = new UpdateSongCommand(1, "new genre", "new song", "new artist", 29,
                "url", Instant.parse("2026-08-30T00:00:00Z"),
                List.of(new UpdateSongCommand.ChartUpdate(10, 42, 29, false, true, true)));

        adapter.update(command, "new-hash");

        assertThat(jdbc.queryForObject("SELECT song_hash FROM songs WHERE song_id=1", String.class)).isEqualTo("new-hash");
        assertThat(jdbc.queryForObject("SELECT level FROM charts WHERE chart_id=10", Integer.class)).isEqualTo(42);
        assertThatThrownBy(() -> adapter.update(new UpdateSongCommand(2, "g", "s", "a", 1,
                null, null, List.of()), "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
