package gg.popn.infra.db.adapter;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.SongHash;
import gg.popn.domain.common.exception.ChartNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.*;

class ChartQueryJdbcAdapterTest {
    @Test
    void readsNormalizedCatalogWithoutLegacyTable() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:chart-query-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE songs(song_id BIGINT PRIMARY KEY, song_hash VARCHAR(64), genre_name VARCHAR(255), song_name VARCHAR(255), version INT, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, song_id BIGINT, difficulty_code INT, difficulty_label VARCHAR(16), level INT, chart_version INT, is_upper BOOLEAN, is_deleted BOOLEAN, created_at TIMESTAMP, updated_at TIMESTAMP)");
        verifyQueries(jdbc);
    }

    static void verifyQueries(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO songs(song_id,song_hash,genre_name,song_name,version,created_at,updated_at) VALUES (1,'hash-one','genre','song',10,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(2,'hash-two','genre2','song2',11,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("""
                INSERT INTO charts(chart_id,song_id,difficulty_code,difficulty_label,level,chart_version,is_upper,is_deleted,created_at,updated_at) VALUES
                (1,1,3,'HYPER',40,20,FALSE,FALSE,'2026-01-01','2026-01-01'),
                (2,1,4,'EX',48,20,FALSE,FALSE,'2026-01-02','2026-01-02'),
                (3,1,4,'EX',49,21,TRUE,FALSE,'2026-01-03','2026-01-03'),
                (4,2,4,'EX',50,21,FALSE,TRUE,'2026-01-04','2026-01-04')
                """);
        var adapter = new ChartQueryJdbcAdapter(jdbc);
        assertThat(adapter.findAllCharts()).hasSize(3);
        assertThat(adapter.findListBySongHash(SongHash.of("hash-one"))).hasSize(3);
        assertThat(adapter.findListBySongHash(SongHash.of("hash-two"))).isEmpty();
        assertThat(adapter.findListBySongHashAndDifficulty(SongHash.of("hash-one"), Difficulty.of(4)))
                .extracting(c -> c.getLevel().getValue()).containsExactly(48, 49);
        var chart = adapter.findBySongHashAndDifficulty(SongHash.of("hash-one"), Difficulty.of(4));
        assertThat(chart.getSongHash().getValue()).isEqualTo("hash-one");
        assertThat(chart.getSongName().getValue()).isEqualTo("song");
        assertThat(chart.getGenreName().getValue()).isEqualTo("genre");
        assertThat(chart.getVersion().getValue()).isEqualTo(10);
        assertThat(chart.getIsUpper().getValue()).isZero();
        assertThat(chart.getDifficulty().getValue()).isEqualTo(4);
        assertThatThrownBy(() -> adapter.findBySongHashAndDifficulty(SongHash.of("hash-two"), Difficulty.of(4)))
                .isInstanceOf(ChartNotFoundException.class);
        assertThat(adapter.findRecentCharts(2)).extracting(c -> c.getLevel().getValue()).containsExactly(49, 48);
        assertThat(adapter.findRecentCharts(0)).isEmpty();
    }
}
