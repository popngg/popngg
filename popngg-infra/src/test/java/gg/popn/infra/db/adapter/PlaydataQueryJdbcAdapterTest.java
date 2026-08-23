package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.query.FindUserRecordsQuery;
import gg.popn.application.playdata.exception.ActualPopclassUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaydataQueryJdbcAdapterTest {
    private JdbcTemplate jdbc;
    private PlaydataQueryJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:query;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE users(user_id BIGINT PRIMARY KEY, poptomo_id VARCHAR(32))
                """);
        jdbc.execute("""
                CREATE TABLE user_profiles(user_id BIGINT PRIMARY KEY, user_name VARCHAR(64),
                  is_hidden BOOLEAN, display_popclass INT, potential_popclass INT,
                  legacy_popclass INT)
                """);
        jdbc.execute("""
                CREATE TABLE songs(song_id BIGINT PRIMARY KEY, song_hash VARCHAR(32),
                  genre_name VARCHAR(255), song_name VARCHAR(255), artist_name VARCHAR(255),
                  version INT, jacket_url VARCHAR(512))
                """);
        jdbc.execute("""
                CREATE TABLE charts(chart_id BIGINT PRIMARY KEY, song_id BIGINT,
                  difficulty_code INT, difficulty_label VARCHAR(16), level INT,
                  chart_version INT, is_upper BOOLEAN, is_deleted BOOLEAN)
                """);
        jdbc.execute("""
                CREATE TABLE playdata(playdata_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT, chart_id BIGINT, current_version INT, version_score INT,
                  version_score_known BOOLEAN DEFAULT TRUE,
                  version_rank_code INT, all_time_score INT, all_time_score_version INT,
                  all_time_rank_code INT, medal_code INT, popclass INT,
                  is_display_popclass_target BOOLEAN, popclass_bucket VARCHAR(20),
                  popclass_bucket_rank INT)
                """);
        seed();
        adapter = new PlaydataQueryJdbcAdapter(jdbc, 29);
    }

    @Test
    void queriesAllReadModels() {
        var user = adapter.findUserPlaydata("0000");
        var levelRank = adapter.count("0000", "LEVEL", "RANK");
        var difficultyMedal = adapter.count("0000", "DIFFICULTY", "MEDAL");
        var records = adapter.findUserRecords("0000", new FindUserRecordsQuery(
                "song", null, 40, 50, List.of(2, 3), null, null,
                90_000, null, "SCORE", "DESC", 0, 20));
        var progress = adapter.findProgress("0000", "LEVEL");
        var popclass = adapter.findPopclass("0000");
        var potential = adapter.findPotentialPopclass("0000");
        var legacyTargets = adapter.findLegacyPopclassTargets("0000");
        var rankings = adapter.findChartRankings(100, 2);

        assertThat(user.playdata()).hasSize(2);
        assertThat(user.playdata().getFirst().versionBest().score()).isEqualTo(95_000);
        assertThat(levelRank.groups()).hasSize(2);
        assertThat(difficultyMedal.groups()).hasSize(2);
        assertThat(records.items()).extracting(row -> row.id())
                .containsExactly("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertThat(records.totalItems()).isEqualTo(2);
        assertThat(records.items()).extracting(row -> row.score())
                .containsExactly(97_000, 90_000);
        assertThat(records.items()).extracting(row -> row.rank())
                .containsExactly(1, 3);
        assertThat(progress.rows()).hasSize(2);
        assertThat(progress.summary().total()).isEqualTo(2);
        assertThat(progress.summary().averageScore()).isEqualTo(93_500);
        assertThat(progress.summary().ranks()).extracting(row -> row.code())
                .containsExactlyInAnyOrder(1, 3);
        assertThat(popclass.targets()).hasSize(1);
        assertThat(potential.targets()).extracting(row -> row.chartId())
                .containsExactly(100L, 101L);
        assertThat(potential.targets().getFirst().popclass()).isGreaterThan(0);
        assertThat(legacyTargets).extracting(row -> row.chartId())
                .containsExactly(100L, 101L);
        assertThat(legacyTargets.getFirst().popclass()).isEqualTo(9_779);
        assertThat(rankings.currentVersion()).extracting(row -> row.poptomoId())
                .containsExactly("0000", "1111");
        assertThat(rankings.allTime()).extracting(row -> row.poptomoId())
                .containsExactly("1111", "0000");
    }

    @Test
    void rejectsMissingUsersAndCharts() {
        assertThatThrownBy(() -> adapter.findUserPlaydata("missing"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findChartRankings(999, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ignoresUnknownVersionBestScoresOutsideActualTargets() {
        jdbc.update("UPDATE playdata SET version_score_known = FALSE WHERE chart_id = 101");

        assertThat(adapter.findPopclass("0000").targets()).hasSize(1);
        assertThat(adapter.findPotentialPopclass("0000").targets()).hasSize(2);
    }

    @Test
    void rejectsActualTableWhenATargetVersionBestScoreIsUnknown() {
        jdbc.update("UPDATE playdata SET version_score_known = FALSE WHERE chart_id = 100");

        assertThatThrownBy(() -> adapter.findPopclass("0000"))
                .isInstanceOf(ActualPopclassUnavailableException.class);
    }

    private void seed() {
        jdbc.update("INSERT INTO users VALUES (1, '0000'), (2, '1111')");
        jdbc.update("""
                INSERT INTO user_profiles VALUES
                    (1, 'first', FALSE, 100, 110, 90),
                    (2, 'second', FALSE, 80, 120, 70)
                """);
        jdbc.update("""
                INSERT INTO songs VALUES
                    (10, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'genre-a', 'song-a',
                     'artist-a', 29, 'jacket-a'),
                    (11, 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 'genre-b', 'song-b',
                     'artist-b', 99, 'jacket-b')
                """);
        jdbc.update("""
                INSERT INTO charts VALUES
                    (100, 10, 3, 'EX', 48, 29, FALSE, FALSE),
                    (101, 11, 2, 'H', 42, 99, FALSE, FALSE)
                """);
        jdbc.update("""
                INSERT INTO playdata
                    (user_id, chart_id, current_version, version_score, version_rank_code,
                     all_time_score, all_time_score_version, all_time_rank_code, medal_code,
                     popclass, is_display_popclass_target, popclass_bucket, popclass_bucket_rank)
                VALUES
                    (1, 100, 29, 95000, 2, 97000, 28, 1, 2,
                     10000, TRUE, 'CURRENT_VERSION', 1),
                    (1, 101, 29, 90000, 3, 90000, 29, 3, 4,
                     9000, FALSE, NULL, NULL),
                    (2, 100, 29, 94000, 3, 99000, 28, 1, 3,
                     9800, FALSE, NULL, NULL)
                """);
    }
}
