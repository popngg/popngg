package gg.popn.infra.db.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PlaydataQueryMySqlIntegrationTest extends MySqlIntegrationTestSupport {
    private JdbcTemplate jdbc;
    private PlaydataQueryJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        var dataSource = mysqlDataSource();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        adapter = new PlaydataQueryJdbcAdapter(jdbc, 29);
        seed();
    }

    @Test
    void queriesUserRowsCountsAndMarkedPopclassTargets() {
        var playdata = adapter.findUserPlaydata("0000-0000-0000");
        var counts = adapter.count("0000-0000-0000", "LEVEL", "RANK");
        var popclass = adapter.findPopclass("0000-0000-0000");

        assertThat(playdata.playdata()).hasSize(2);
        assertThat(playdata.playdata().getFirst().versionBest().score()).isEqualTo(95_000);
        assertThat(playdata.playdata().getFirst().allTimeBest().score()).isEqualTo(97_000);
        assertThat(playdata.playdata().getFirst().medal().code()).isEqualTo(2);
        assertThat(counts.groups()).extracting(group -> group.count())
                .containsExactlyInAnyOrder(1L, 1L);
        assertThat(popclass.targets()).hasSize(1);
        assertThat(popclass.targets().getFirst().popclassBucket())
                .isEqualTo("CURRENT_VERSION");
    }

    @Test
    void limitsRowsBeforeJoiningProfilesAndSeparatesCurrentFromAllTime() {
        var rankings = adapter.findChartRankings(100, 2);

        assertThat(rankings.currentVersion()).extracting(entry -> entry.poptomoId())
                .containsExactly("0000-0000-0000", "1111-1111-1111");
        assertThat(rankings.allTime()).extracting(entry -> entry.poptomoId())
                .containsExactly("1111-1111-1111", "0000-0000-0000");
        assertThat(rankings.currentVersion()).extracting(entry -> entry.ranking())
                .containsExactly(1, 2);
    }

    private void seed() {
        jdbc.update("""
                INSERT INTO users
                    (user_id, poptomo_id, password_hash, role, created_at, updated_at)
                VALUES
                    (1, '0000-0000-0000', 'test-only', 'USER', NOW(), NOW()),
                    (2, '1111-1111-1111', 'test-only', 'USER', NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO user_profiles
                    (user_id, user_name, character_name, comment, is_hidden,
                     display_popclass, potential_popclass, legacy_popclass,
                     created_at, updated_at)
                VALUES
                    (1, 'first', '', '', FALSE, 100, 110, 90, NOW(), NOW()),
                    (2, 'second', '', '', FALSE, 80, 120, 70, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO songs
                    (song_id, song_hash, genre_name, song_name, version, created_at, updated_at)
                VALUES
                    (10, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'genre-a', 'song-a', 29, NOW(), NOW()),
                    (11, 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 'genre-b', 'song-b', 28, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO charts
                    (chart_id, song_id, difficulty_code, difficulty_label, level,
                     chart_version, is_upper, created_at, updated_at)
                VALUES
                    (100, 10, 3, 'EX', 48, 29, FALSE, NOW(), NOW()),
                    (101, 11, 2, 'H', 42, 28, FALSE, NOW(), NOW())
                """);
        jdbc.update("""
                INSERT INTO playdata
                    (user_id, chart_id, current_version, version_score, version_rank_code,
                     all_time_score, all_time_score_version, all_time_rank_code, medal_code,
                     popclass, is_display_popclass_target, popclass_bucket,
                     popclass_bucket_rank, created_at, updated_at)
                VALUES
                    (1, 100, 29, 95000, 2, 97000, 28, 1, 2,
                     10000, TRUE, 'CURRENT_VERSION', 1, NOW(), NOW()),
                    (1, 101, 29, 90000, 3, 90000, 29, 3, 4,
                     9000, FALSE, NULL, NULL, NOW(), NOW()),
                    (2, 100, 29, 94000, 3, 99000, 28, 1, 3,
                     9800, FALSE, NULL, NULL, NOW(), NOW())
                """);
    }
}
