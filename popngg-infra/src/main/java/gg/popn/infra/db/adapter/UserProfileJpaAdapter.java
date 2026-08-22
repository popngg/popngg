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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
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
                                + "order by p." + sortProperty + " desc, u.poptomoId asc",
                        UserEntity.class)
                .setFirstResult(query.page() * query.size())
                .setMaxResults(query.size())
                .getResultList();
        long total = entityManager.createQuery(
                        "select count(u) from UserEntity u join u.profile p where p.hidden = false",
                        Long.class)
                .getSingleResult();
        return new RankingPage(users.stream().map(this::toResult).toList(), total);
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
        List<Object> args = new ArrayList<>();
        if (!keywordClause.isEmpty()) {
            String keyword = "%" + query.keyword().trim() + "%";
            args.add(keyword);
            args.add(keyword);
        }
        String base = """
                SELECT u.user_id, u.poptomo_id, p.user_name, p.profile_image_url,
                       p.comment, p.display_popclass, p.updated_at,
                       ROW_NUMBER() OVER (
                           ORDER BY p.display_popclass DESC, u.poptomo_id ASC
                       ) AS ranking,
                       COALESCE((
                           SELECT MAX(c.level)
                             FROM playdata pd JOIN charts c ON c.chart_id = pd.chart_id
                            WHERE pd.user_id = u.user_id
                              AND pd.current_version = ? AND c.is_deleted = FALSE
                              AND pd.medal_code IN (1,2,3,4,5,6,7,11,12)
                       ), 0) AS clear_level
                  FROM users u JOIN user_profiles p ON p.user_id = u.user_id
                 WHERE p.is_hidden = FALSE
                """;
        List<Object> baseArgs = new ArrayList<>();
        baseArgs.add(currentVersion);
        baseArgs.addAll(args);
        String sql = "SELECT * FROM (" + base + ") ranked" + keywordClause
                + " ORDER BY " + sortColumn + " " + direction
                + ", poptomo_id ASC LIMIT ? OFFSET ?";
        baseArgs.add(query.size());
        baseArgs.add(query.page() * query.size());
        List<UserListRow> rows = jdbc.query(sql, (rs, rowNum) -> new UserListRow(
                rs.getLong("user_id"), rs.getString("poptomo_id"),
                rs.getString("user_name"), rs.getString("profile_image_url"),
                rs.getString("comment"), rs.getInt("display_popclass"),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getInt("ranking")), baseArgs.toArray());
        List<Object> countArgs = new ArrayList<>();
        countArgs.add(currentVersion);
        countArgs.addAll(args);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" + base + ") ranked" + keywordClause,
                Long.class, countArgs.toArray());
        return new UserListResult(rows.stream().map(row -> new UserListResult.Item(
                row.poptomoId(), row.userName(), row.profileImageUrl(), row.comment(),
                row.rank(), row.displayPopclass(), medalSummaries(row.userId()),
                row.updatedAt())).toList(), query.page(), query.size(),
                total == null ? 0 : total);
    }

    private UserProfileResult toResult(UserEntity user) {
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
                medalSummaries(user.getId()),
                profile.getUpdatedAt());
    }

    private List<UserProfileResult.MedalSummary> medalSummaries(Long userId) {
        List<LevelMedalCounts> levels = jdbc.query("""
                SELECT c.level,
                       COUNT(*) AS total_count,
                       SUM(CASE WHEN p.medal_code IN (1,2,3,4,5,6,7,11,12)
                                THEN 1 ELSE 0 END) AS clear_count,
                       SUM(CASE WHEN p.medal_code IN (1,2,3,4)
                                THEN 1 ELSE 0 END) AS full_combo_count,
                       SUM(CASE WHEN p.medal_code = 1
                                THEN 1 ELSE 0 END) AS perfect_count
                  FROM charts c
                  LEFT JOIN playdata p
                    ON p.chart_id = c.chart_id
                   AND p.user_id = ?
                   AND p.current_version = ?
                 WHERE c.is_deleted = FALSE
                 GROUP BY c.level
                 ORDER BY c.level
                """, (rs, rowNum) -> new LevelMedalCounts(
                        rs.getInt("level"),
                        rs.getLong("total_count"),
                        rs.getLong("clear_count"),
                        rs.getLong("full_combo_count"),
                        rs.getLong("perfect_count")),
                userId, currentVersion);
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

    private record UserListRow(
            long userId, String poptomoId, String userName, String profileImageUrl,
            String comment, int displayPopclass, LocalDateTime updatedAt, int rank
    ) {
    }
}
