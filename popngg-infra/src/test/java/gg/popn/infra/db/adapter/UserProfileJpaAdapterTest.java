package gg.popn.infra.db.adapter;

import gg.popn.infra.db.entity.UserEntity;
import gg.popn.infra.db.entity.UserProfileEntity;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.infra.db.jpa.UserJpaRepository;
import gg.popn.infra.db.jpa.UserProfileJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileJpaAdapterTest {
    private UserJpaRepository userRepository;
    private JdbcTemplate jdbc;
    private EntityManager entityManager;
    private UserProfileJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:user-profile;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa", "");
        jdbc = spy(new JdbcTemplate(source));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE users(
                  user_id BIGINT PRIMARY KEY, poptomo_id VARCHAR(32) NOT NULL)
                """);
        jdbc.execute("""
                CREATE TABLE user_profiles(
                  user_id BIGINT PRIMARY KEY, user_name VARCHAR(64),
                  profile_image_url VARCHAR(512), comment VARCHAR(255),
                  display_popclass INT, potential_popclass INT DEFAULT 0,
                  is_hidden BOOLEAN, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE charts(
                  chart_id BIGINT PRIMARY KEY, level INT NOT NULL, is_deleted BOOLEAN NOT NULL)
                """);
        jdbc.execute("""
                CREATE TABLE playdata(
                  user_id BIGINT NOT NULL, chart_id BIGINT NOT NULL,
                  current_version INT NOT NULL, medal_code INT NOT NULL)
                """);
        jdbc.update("""
                INSERT INTO charts(chart_id, level, is_deleted) VALUES
                  (1, 48, FALSE), (2, 48, FALSE),
                  (3, 49, FALSE), (4, 49, FALSE),
                  (5, 50, FALSE), (6, 50, TRUE),
                  (7, 47, FALSE), (8, 46, FALSE)
                """);

        userRepository = mock(UserJpaRepository.class);
        entityManager = mock(EntityManager.class);
        adapter = new UserProfileJpaAdapter(
                userRepository,
                mock(UserProfileJpaRepository.class),
                entityManager,
                jdbc,
                29);
    }

    @Test
    void returnsHighestLevelSummaryForEachMedalKindAndUpdatedAt() {
        var updatedAt = LocalDateTime.of(2026, 8, 22, 12, 34, 56);
        when(userRepository.findByPoptomoId("1234-5678-9012"))
                .thenReturn(Optional.of(user(10L, "1234-5678-9012", updatedAt)));
        jdbc.update("""
                INSERT INTO playdata(user_id, chart_id, current_version, medal_code) VALUES
                  (10, 1, 29, 1),
                  (10, 3, 29, 2),
                  (10, 5, 29, 7),
                  (10, 7, 29, 11),
                  (10, 8, 29, 8),
                  (10, 4, 28, 1)
                """);

        var result = adapter.findByPoptomoId("1234-5678-9012").orElseThrow();

        assertThat(result.medalSummaries()).containsExactly(
                new gg.popn.application.user.dto.result.UserProfileResult.MedalSummary(
                        "clear", 50, 1, 1),
                new gg.popn.application.user.dto.result.UserProfileResult.MedalSummary(
                        "full-combo", 49, 1, 2),
                new gg.popn.application.user.dto.result.UserProfileResult.MedalSummary(
                        "perfect", 48, 1, 2));
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void returnsZeroSummariesWhenUserHasNoAchievements() {
        when(userRepository.findByPoptomoId("0000-0000-0000"))
                .thenReturn(Optional.of(user(20L, "0000-0000-0000", LocalDateTime.now())));

        var summaries = adapter.findByPoptomoId("0000-0000-0000")
                .orElseThrow().medalSummaries();

        assertThat(summaries).allSatisfy(summary -> {
            assertThat(summary.maxLevel()).isZero();
            assertThat(summary.achieved()).isZero();
            assertThat(summary.total()).isZero();
        });
    }

    @Test
    void treatsEasyAsClearAndBlackMedalsAsFailed() {
        when(userRepository.findByPoptomoId("1111-1111-1111"))
                .thenReturn(Optional.of(user(30L, "1111-1111-1111", LocalDateTime.now())));
        jdbc.update("""
                INSERT INTO playdata(user_id, chart_id, current_version, medal_code) VALUES
                  (30, 7, 29, 11),
                  (30, 5, 29, 8),
                  (30, 3, 29, 4)
                """);

        var summaries = adapter.findByPoptomoId("1111-1111-1111")
                .orElseThrow().medalSummaries();

        assertThat(summaries).containsExactly(
                new gg.popn.application.user.dto.result.UserProfileResult.MedalSummary(
                        "clear", 49, 1, 2),
                new gg.popn.application.user.dto.result.UserProfileResult.MedalSummary(
                        "full-combo", 49, 1, 2),
                new gg.popn.application.user.dto.result.UserProfileResult.MedalSummary(
                        "perfect", 0, 0, 0));
    }

    @Test
    void findsPublicUsersUsingFrontendSearchSortAndPagination() {
        jdbc.update("INSERT INTO users VALUES (10, '1234-5678-9012'), (20, '9999-9999-9999')");
        jdbc.update("""
                INSERT INTO user_profiles VALUES
                  (10, 'alpha', NULL, 'first', 0, 180000, FALSE, TIMESTAMP '2026-08-22 10:00:00'),
                  (20, 'hidden', NULL, 'second', 200000, 201000, TRUE, TIMESTAMP '2026-08-22 11:00:00')
                """);
        jdbc.update("""
                INSERT INTO playdata(user_id, chart_id, current_version, medal_code) VALUES
                  (10, 1, 29, 1), (10, 3, 29, 5)
                """);

        var result = adapter.findUsers(new FindUsersQuery(
                "alpha", FindUsersQuery.Sort.RANK,
                FindUsersQuery.Order.ASC, 0, 20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.users()).singleElement().satisfies(user -> {
            assertThat(user.poptomoId()).isEqualTo("1234-5678-9012");
            assertThat(user.rank()).isEqualTo(1);
            assertThat(user.displayPopclass()).isEqualTo(180000);
            assertThat(user.bestLevels()).extracting(summary -> summary.maxLevel())
                    .containsExactly(49, 48, 48);
        });
    }

    @Test
    void loadsMedalSummariesForThePageInOneQuery() {
        jdbc.update("INSERT INTO users VALUES (10, '1234-5678-9012'), (20, '9999-9999-9999')");
        jdbc.update("""
                INSERT INTO user_profiles VALUES
                  (10, 'alpha', NULL, 'first', 177000, 180000, FALSE, TIMESTAMP '2026-08-22 10:00:00'),
                  (20, 'beta', NULL, 'second', 176000, 179000, FALSE, TIMESTAMP '2026-08-22 11:00:00')
                """);
        jdbc.update("""
                INSERT INTO playdata(user_id, chart_id, current_version, medal_code) VALUES
                  (10, 1, 29, 1), (20, 3, 29, 5)
                """);

        var result = adapter.findUsers(new FindUsersQuery(
                null, FindUsersQuery.Sort.RANK,
                FindUsersQuery.Order.ASC, 0, 20));

        assertThat(result.users()).hasSize(2);
        assertThat(result.users().get(0).bestLevels().get(0).maxLevel()).isEqualTo(48);
        assertThat(result.users().get(1).bestLevels().get(0).maxLevel()).isEqualTo(49);
        verify(jdbc, times(2)).query(
                anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                any(Object[].class));
    }

    @Test
    void loadsRankingMedalSummariesOnceForTheRequestedPage() {
        var first = user(10L, "1234-5678-9012", LocalDateTime.now());
        var second = user(20L, "9999-9999-9999", LocalDateTime.now(), 0, 200);
        @SuppressWarnings("unchecked")
        TypedQuery<UserEntity> usersQuery = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        TypedQuery<Long> countQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(UserEntity.class))).thenReturn(usersQuery);
        when(usersQuery.setFirstResult(0)).thenReturn(usersQuery);
        when(usersQuery.setMaxResults(20)).thenReturn(usersQuery);
        when(usersQuery.getResultList()).thenReturn(java.util.List.of(first, second));
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(2L);

        var result = adapter.findRankings(new UserRankingQuery(
                UserRankingQuery.Sort.DISPLAY_POPCLASS, 0, 20));

        assertThat(result.users()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.users().get(1).displayPopclass()).isEqualTo(200);
        verify(jdbc, times(1)).query(
                anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                any(Object[].class));
    }

    private static UserEntity user(Long id, String poptomoId, LocalDateTime updatedAt) {
        return user(id, poptomoId, updatedAt, 100, 200);
    }

    private static UserEntity user(Long id, String poptomoId, LocalDateTime updatedAt,
                                   int displayPopclass, int potentialPopclass) {
        var profile = UserProfileEntity.builder()
                .userId(id)
                .userName("name")
                .characterName("character")
                .comment("comment")
                .profileImageUrl(null)
                .hidden(false)
                .displayPopclass(displayPopclass)
                .potentialPopclass(potentialPopclass)
                .legacyPopclass(300)
                .normalCredit(1)
                .extraCredit(2)
                .timePlay10Credit(3)
                .timePlay16Credit(4)
                .createdAt(updatedAt)
                .updatedAt(updatedAt)
                .build();
        return UserEntity.builder()
                .id(id)
                .poptomoId(poptomoId)
                .passwordHash("hash")
                .role("USER")
                .createdAt(updatedAt)
                .updatedAt(updatedAt)
                .profile(profile)
                .build();
    }
}
