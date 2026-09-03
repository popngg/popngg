package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.command.CreateSongCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateSongJdbcAdapterTest {
    private JdbcTemplate jdbc;
    private CreateSongJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:create-song-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        UserDirectoryTestSchema.create(jdbc);
        adapter = new CreateSongJdbcAdapter(new NamedParameterJdbcTemplate(jdbc));
        jdbc.execute("""
                CREATE TABLE songs (
                    song_id BIGINT AUTO_INCREMENT PRIMARY KEY, song_hash VARCHAR(32),
                    genre_name VARCHAR(255) NOT NULL, song_name VARCHAR(255) NOT NULL,
                    artist_name VARCHAR(255), version INT NOT NULL, jacket_url VARCHAR(512),
                    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE charts (
                    chart_id BIGINT AUTO_INCREMENT PRIMARY KEY, song_id BIGINT NOT NULL,
                    difficulty_code INT NOT NULL, difficulty_label VARCHAR(16) NOT NULL,
                    level INT NOT NULL, chart_version INT NOT NULL,
                    has_strict_judgement BOOLEAN NOT NULL, has_strict_gauge BOOLEAN NOT NULL,
                    is_upper BOOLEAN NOT NULL, is_deleted BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
                    UNIQUE(song_id, difficulty_code, is_upper)
                )
                """);
    }

    @Test
    void insertsSeparatedSongAndChartsAndReturnsIds() {
        CreateSongCommand command = command(List.of(chart(false), chart(true)));

        var result = adapter.create(command);

        assertThat(result.songId()).isPositive();
        assertThat(result.chartIds()).hasSize(2).allMatch(id -> id > 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM songs", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM charts", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT difficulty_label FROM charts WHERE is_upper = FALSE", String.class))
                .isEqualTo("LIGHT");
    }

    @Test
    void databaseConstraintRejectsDuplicateChart() {
        assertThatThrownBy(() -> adapter.create(command(List.of(chart(false), chart(false)))))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private CreateSongCommand command(List<CreateSongCommand.CreateChartCommand> charts) {
        return new CreateSongCommand("hash", "genre", "song", "artist", 28, null, charts);
    }

    private CreateSongCommand.CreateChartCommand chart(boolean upper) {
        return new CreateSongCommand.CreateChartCommand(1, 45, 28, upper, true, false);
    }
}
