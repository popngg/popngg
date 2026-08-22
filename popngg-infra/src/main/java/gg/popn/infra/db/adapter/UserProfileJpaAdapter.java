package gg.popn.infra.db.adapter;

import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
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
}
