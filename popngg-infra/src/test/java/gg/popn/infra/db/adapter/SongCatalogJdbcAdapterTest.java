package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.query.FindSongsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class SongCatalogJdbcAdapterTest {
    private JdbcTemplate jdbcTemplate;

    private SongCatalogJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:song-catalog-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        adapter = new SongCatalogJdbcAdapter(new NamedParameterJdbcTemplate(jdbcTemplate));
        jdbcTemplate.execute("""
                CREATE TABLE songs (
                    song_id BIGINT PRIMARY KEY, song_hash VARCHAR(32), genre_name VARCHAR(255),
                    song_name VARCHAR(255), artist_name VARCHAR(255), version INT, jacket_url VARCHAR(512)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE charts (
                    chart_id BIGINT PRIMARY KEY, song_id BIGINT, difficulty_code INT,
                    difficulty_label VARCHAR(16), level INT, chart_version INT,
                    has_strict_judgement BOOLEAN, has_strict_gauge BOOLEAN,
                    is_upper BOOLEAN, is_deleted BOOLEAN
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE song_search_tags (
                    tag_id BIGINT PRIMARY KEY, song_id BIGINT, normalized_tag_value VARCHAR(255),
                    is_active BOOLEAN
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO songs VALUES
                (1, 'hash-1', 'High☆Cheers', 'Moon Child', 'Artist', 20, '/jacket/1'),
                (2, 'hash-2', 'Other', 'Another Song', NULL, 28, NULL)
                """);
        jdbcTemplate.update("""
                INSERT INTO charts VALUES
                (10, 1, 1, 'EASY', 45, 28, TRUE, FALSE, FALSE, FALSE),
                (11, 1, 4, 'EX', 49, 20, FALSE, TRUE, TRUE, FALSE),
                (20, 2, 2, 'NORMAL', 30, 28, FALSE, FALSE, FALSE, FALSE)
                """);
        jdbcTemplate.update("""
                INSERT INTO song_search_tags VALUES
                (100, 1, '문차일드', TRUE),
                (101, 2, '비활성', FALSE)
                """);
    }

    @Test
    void searchesActiveAliasAndUsesLightLabel() {
        FindSongsQuery query = query("문차일드", null, null);

        var result = adapter.findPage(query);

        assertThat(adapter.count(query)).isEqualTo(1);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().genreName()).isEqualTo("High☆Cheers");
        assertThat(result.getFirst().charts().getFirst().difficultyLabel()).isEqualTo("LIGHT");
    }

    @Test
    void filtersUsingChartVersionAndReturnsOnlyMatchingCharts() {
        FindSongsQuery query = query(null, 28, false);

        var result = adapter.findPage(query);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().charts())
                .allMatch(chart -> chart.chartVersion() == 28 && !chart.isUpper());
    }

    private FindSongsQuery query(String keyword, Integer chartVersion, Boolean isUpper) {
        return new FindSongsQuery(keyword, null, chartVersion, null, null,
                isUpper, null, null, 0, 20);
    }
}
