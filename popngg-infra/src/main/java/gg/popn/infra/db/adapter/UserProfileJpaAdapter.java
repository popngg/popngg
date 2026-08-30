package gg.popn.infra.db.adapter;

import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.dto.result.UserListResult;
import gg.popn.application.user.exception.UserProfileNotFoundException;
import gg.popn.application.user.port.out.UserProfilePort;
import gg.popn.infra.db.entity.UserEntity;
import gg.popn.infra.db.jpa.UserJpaRepository;
import gg.popn.infra.db.jpa.UserProfileJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserProfileJpaAdapter implements UserProfilePort {
    private final UserJpaRepository userRepository;
    private final UserProfileJpaRepository profileRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final int currentVersion;

    public UserProfileJpaAdapter(
            UserJpaRepository userRepository,
            UserProfileJpaRepository profileRepository,
            EntityManager entityManager,
            JdbcTemplate jdbc,
            @Value("${popngg.game.current-version:29}") int currentVersion) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.entityManager = entityManager;
        this.jdbc = jdbc;
        this.currentVersion = currentVersion;
    }

    @Override
    public Optional<UserProfileResult> findByPoptomoId(String poptomoId) {
        return userRepository.findByPoptomoId(poptomoId).map(this::toResult);
    }

    @Override
    public UserProfileResult update(UpdateUserProfileCommand command) {
        var user = userRepository.findByPoptomoId(command.poptomoId())
                .orElseThrow(UserProfileNotFoundException::new);
        user.getProfile().update(
                command.userName(),
                command.characterName(),
                command.comment(),
                command.profileImageUrl(),
                command.hidden(),
                LocalDateTime.now());
        profileRepository.save(user.getProfile());
        return toResult(user);
    }

    @Override
    public RankingPage findRankings(UserRankingQuery query) {
        String sortProperty = switch (query.sort()) {
            case DISPLAY_POPCLASS -> "displayPopclass";
            case POTENTIAL_POPCLASS -> "potentialPopclass";
            case LEGACY_POPCLASS -> "legacyPopclass";
        };
        List<UserEntity> users = entityManager.createQuery(
                        "select u from UserEntity u join fetch u.profile p "
                                + "where p.hidden = false "
                                + "order by "
                                + (query.sort() == UserRankingQuery.Sort.DISPLAY_POPCLASS
                                    ? "case when p.displayPopclass = 0 then p.potentialPopclass "
                                      + "else p.displayPopclass end"
                                    : "p." + sortProperty)
                                + " desc, u.poptomoId asc",
                        UserEntity.class)
                .setFirstResult(query.page() * query.size())
                .setMaxResults(query.size())
                .getResultList();
        long total = entityManager.createQuery(
                        "select count(u) from UserEntity u join u.profile p where p.hidden = false",
                        Long.class)
                .getSingleResult();
        Map<Long, List<UserProfileResult.MedalSummary>> summariesByUser = medalSummaries(
                users.stream().map(UserEntity::getId).toList());
        return new RankingPage(users.stream()
                .map(user -> rankingResult(user, summariesByUser.get(user.getId())))
                .toList(), total);
    }

    @Override
    public UserListResult findUsers(FindUsersQuery query) {
        String sortColumn = switch (query.sort()) {
            case RANK -> "ranking";
            case NAME -> "user_name";
            case CLEAR_LEVEL -> "clear_level";
            case UPDATED_AT -> "updated_at";
        };
        String direction = query.order().name();
        String keywordClause = query.keyword() == null || query.keyword().isBlank()
                ? "" : " WHERE ranked.user_name LIKE ? OR ranked.poptomo_id LIKE ?";
        String directKeywordClause = query.keyword() == null || query.keyword().isBlank()
                ? "" : " AND (p.user_name LIKE ? OR u.poptomo_id LIKE ?)";
        List<Object> args = new ArrayList<>();
        if (!keywordClause.isEmpty()) {
            String keyword = "%" + query.keyword().trim() + "%";
            args.add(keyword);
            args.add(keyword);
        }
        boolean needsClearLevel = query.sort() == FindUsersQuery.Sort.CLEAR_LEVEL;
        String clearLevel = needsClearLevel ? """
                       COALESCE((
                           SELECT MAX(c.level)
                             FROM playdata pd JOIN charts c ON c.chart_id = pd.chart_id
                            WHERE pd.user_id = u.user_id
                              AND pd.current_version = ? AND c.is_deleted = FALSE
                              AND pd.medal_code IN (1,2,3,4,5,6,7,11,12)
                       ), 0) AS clear_level
                """ : "0 AS clear_level";
        String base = """
                SELECT u.user_id, u.poptomo_id, p.user_name, p.profile_image_url,
                       p.comment,
                       CASE WHEN p.display_popclass = 0 THEN p.potential_popclass
                            ELSE p.display_popclass END AS effective_popclass,
                       p.updated_at,
                       ROW_NUMBER() OVER (
                           ORDER BY CASE WHEN p.display_popclass = 0
                                             THEN p.potential_popclass
                                         ELSE p.display_popclass END DESC,
                                    u.poptomo_id ASC
                       ) AS ranking,
                       %s
                  FROM users u JOIN user_profiles p ON p.user_id = u.user_id
                 WHERE p.is_hidden = FALSE
                """.formatted(clearLevel);
        List<Object> baseArgs = new ArrayList<>();
        if (needsClearLevel) baseArgs.add(currentVersion);
        baseArgs.addAll(args);
        String sql = "SELECT * FROM (" + base + ") ranked" + keywordClause
                + " ORDER BY " + sortColumn + " " + direction
                + ", poptomo_id ASC LIMIT ? OFFSET ?";
        baseArgs.add(query.size());
        baseArgs.add(query.page() * query.size());
        List<UserListRow> rows = jdbc.query(sql, (rs, rowNum) -> new UserListRow(
                rs.getLong("user_id"), rs.getString("poptomo_id"),
                rs.getString("user_name"), rs.getString("profile_image_url"),
                rs.getString("comment"), rs.getInt("effective_popclass"),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getInt("ranking")), baseArgs.toArray());
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN user_profiles p ON p.user_id = u.user_id "
                        + "WHERE p.is_hidden = FALSE" + directKeywordClause,
                Long.class, args.toArray());
        Map<Long, List<UserProfileResult.MedalSummary>> summariesByUser = medalSummaries(
                rows.stream().map(UserListRow::userId).toList());
        return new UserListResult(rows.stream().map(row -> new UserListResult.Item(
                row.poptomoId(), row.userName(), row.profileImageUrl(), row.comment(),
                row.rank(), row.displayPopclass(), summariesByUser.get(row.userId()),
                row.updatedAt())).toList(), query.page(), query.size(),
                total == null ? 0 : total);
    }

    private UserProfileResult toResult(UserEntity user) {
        return toResult(user, medalSummaries(user.getId()));
    }

    private UserProfileResult rankingResult(
            UserEntity user, List<UserProfileResult.MedalSummary> medalSummaries) {
        UserProfileResult result = toResult(user, medalSummaries);
        if (result.displayPopclass() != 0) return result;
        return new UserProfileResult(
                result.poptomoId(), result.userName(), result.characterName(),
                result.comment(), result.profileImageUrl(), result.hidden(),
                result.potentialPopclass(), result.potentialPopclass(),
                result.legacyPopclass(), result.normalCredit(), result.extraCredit(),
                result.timePlay10Credit(), result.timePlay16Credit(),
                result.medalSummaries(), result.updatedAt());
    }

    private UserProfileResult toResult(
            UserEntity user, List<UserProfileResult.MedalSummary> medalSummaries) {
        var profile = user.getProfile();
        return new UserProfileResult(
                user.getPoptomoId(),
                profile.getUserName(),
                profile.getCharacterName(),
                profile.getComment(),
                profile.getProfileImageUrl(),
                profile.isHidden(),
                profile.getDisplayPopclass(),
                profile.getPotentialPopclass(),
                profile.getLegacyPopclass(),
                profile.getNormalCredit(),
                profile.getExtraCredit(),
                profile.getTimePlay10Credit(),
                profile.getTimePlay16Credit(),
                medalSummaries,
                profile.getUpdatedAt());
    }

    private List<UserProfileResult.MedalSummary> medalSummaries(Long userId) {
        return medalSummaries(List.of(userId)).get(userId);
    }

    private Map<Long, List<UserProfileResult.MedalSummary>> medalSummaries(
            List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        String selectedUsers = IntStream.range(0, userIds.size())
                .mapToObj(index -> "SELECT CAST(? AS DECIMAL(20,0)) AS user_id")
                .collect(Collectors.joining(" UNION ALL "));
        List<Object> args = new ArrayList<>(userIds);
        args.add(currentVersion);
        List<UserLevelMedalCounts> levels = jdbc.query("""
                SELECT selected_users.user_id, c.level,
                       COUNT(*) AS total_count,
                       SUM(CASE WHEN p.medal_code IN (1,2,3,4,5,6,7,11,12)
                                THEN 1 ELSE 0 END) AS clear_count,
                       SUM(CASE WHEN p.medal_code IN (1,2,3,4)
                                THEN 1 ELSE 0 END) AS full_combo_count,
                       SUM(CASE WHEN p.medal_code = 1
                                THEN 1 ELSE 0 END) AS perfect_count
                  FROM (%s) selected_users
                 CROSS JOIN charts c
                  LEFT JOIN playdata p
                    ON p.chart_id = c.chart_id
                   AND p.user_id = selected_users.user_id
                   AND p.current_version = ?
                 WHERE c.is_deleted = FALSE
                 GROUP BY selected_users.user_id, c.level
                 ORDER BY selected_users.user_id, c.level
                """.formatted(selectedUsers), (rs, rowNum) -> new UserLevelMedalCounts(
                        rs.getLong("user_id"),
                        rs.getInt("level"),
                        rs.getLong("total_count"),
                        rs.getLong("clear_count"),
                        rs.getLong("full_combo_count"),
                        rs.getLong("perfect_count")),
                args.toArray());
        Map<Long, List<LevelMedalCounts>> levelsByUser = new HashMap<>();
        for (UserLevelMedalCounts level : levels) {
            levelsByUser.computeIfAbsent(level.userId(), ignored -> new ArrayList<>())
                    .add(level.counts());
        }
        Map<Long, List<UserProfileResult.MedalSummary>> summariesByUser = new HashMap<>();
        for (Long userId : userIds) {
            summariesByUser.put(userId, summaries(
                    levelsByUser.getOrDefault(userId, List.of())));
        }
        return summariesByUser;
    }

    private List<UserProfileResult.MedalSummary> summaries(List<LevelMedalCounts> levels) {
        return List.of(
                summary("clear", levels, LevelMedalCounts::clear),
                summary("full-combo", levels, LevelMedalCounts::fullCombo),
                summary("perfect", levels, LevelMedalCounts::perfect));
    }

    private UserProfileResult.MedalSummary summary(
            String kind,
            List<LevelMedalCounts> levels,
            java.util.function.ToLongFunction<LevelMedalCounts> achieved) {
        return levels.stream()
                .filter(level -> achieved.applyAsLong(level) > 0)
                .max(Comparator.comparingInt(LevelMedalCounts::level))
                .map(level -> new UserProfileResult.MedalSummary(
                        kind, level.level(), achieved.applyAsLong(level), level.total()))
                .orElseGet(() -> new UserProfileResult.MedalSummary(kind, 0, 0, 0));
    }

    private record LevelMedalCounts(
            int level,
            long total,
            long clear,
            long fullCombo,
            long perfect
    ) {
    }

    private record UserLevelMedalCounts(
            long userId,
            int level,
            long total,
            long clear,
            long fullCombo,
            long perfect
    ) {
        private LevelMedalCounts counts() {
            return new LevelMedalCounts(level, total, clear, fullCombo, perfect);
        }
    }

    private record UserListRow(
            long userId, String poptomoId, String userName, String profileImageUrl,
            String comment, int displayPopclass, LocalDateTime updatedAt, int rank
    ) {
    }
}
