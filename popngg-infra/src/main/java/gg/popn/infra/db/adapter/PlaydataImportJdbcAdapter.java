package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;
import gg.popn.application.playdata.port.out.PopclassRecalculationPort;
import gg.popn.application.playdata.port.out.PlaydataImportPort;
import gg.popn.application.playdata.service.PlaydataUpsertPolicy;
import gg.popn.application.playdata.service.PlaydataHistoryPolicy;
import gg.popn.application.playdata.service.PopclassPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@Component
public class PlaydataImportJdbcAdapter implements PlaydataImportPort, PopclassRecalculationPort {
    private final JdbcTemplate jdbc;
    private final PlaydataUpsertPolicy upsertPolicy;
    private final PlaydataHistoryPolicy historyPolicy;
    private final PopclassPolicy popclassPolicy;
    private final int currentVersion;
    private final TransactionTemplate independentTransaction;

    @Autowired
    public PlaydataImportJdbcAdapter(JdbcTemplate jdbc, PlaydataUpsertPolicy upsertPolicy,
                                     PlaydataHistoryPolicy historyPolicy,
                                     PopclassPolicy popclassPolicy,
                                     @Value("${popngg.game.current-version:29}") int currentVersion,
                                     PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.upsertPolicy = upsertPolicy;
        this.historyPolicy = historyPolicy;
        this.popclassPolicy = popclassPolicy;
        this.currentVersion = currentVersion;
        this.independentTransaction = new TransactionTemplate(transactionManager);
        this.independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    PlaydataImportJdbcAdapter(JdbcTemplate jdbc, PlaydataUpsertPolicy upsertPolicy,
                              PlaydataHistoryPolicy historyPolicy, PopclassPolicy popclassPolicy,
                              int currentVersion) {
        this.jdbc = jdbc;
        this.upsertPolicy = upsertPolicy;
        this.historyPolicy = historyPolicy;
        this.popclassPolicy = popclassPolicy;
        this.currentVersion = currentVersion;
        this.independentTransaction = null;
    }

    @Override
    @Transactional
    public ImportPlaydataResult execute(ImportPlaydataCommand command) {
        long userId = findUserId(command.poptomoId());
        Integer previousDisplayPopclass = findDisplayPopclass(userId);
        updateProfile(userId, command.profile());
        long renewLogId = startLog(command, userId);
        var unmatched = new ArrayList<ImportPlaydataResult.UnmatchedRow>();
        int matched = 0;
        int updated = 0;
        int histories = 0;
        int recordsAdded = 0;
        int medalsImproved = 0;
        int scoresImproved = 0;
        try {
            for (int index = 0; index < command.rows().size(); index++) {
                Match match = match(command.rows().get(index));
                if (match.chartId() == null) {
                    unmatched.add(new ImportPlaydataResult.UnmatchedRow(index, match.reason()));
                } else {
                    matched++;
                    UpsertOutcome outcome = upsert(
                            userId, match.chartId(), command.rows().get(index), renewLogId);
                    if (outcome.updated()) {
                        updated++;
                    }
                    histories += outcome.historyCount();
                    if (outcome.recordAdded()) recordsAdded++;
                    if (outcome.medalImproved()) medalsImproved++;
                    if (outcome.scoreImproved()) scoresImproved++;
                }
            }
            var popclass = rebuildPopclass(command.poptomoId(), userId,
                    command.profile() == null ? null : command.profile().displayPopclass());
            Integer popnClassDelta = previousDisplayPopclass == null
                    ? null : popclass.displayPopclass() - previousDisplayPopclass;
            String status = unmatched.isEmpty() ? "SUCCESS" : matched == 0 ? "FAILED" : "PARTIAL_SUCCESS";
            finishLog(renewLogId, status, matched, updated,
                    unmatched.isEmpty() ? null : summarize(unmatched));
            return new ImportPlaydataResult(renewLogId, command.rows().size(), matched,
                    updated, histories, unmatched.size(), recordsAdded, medalsImproved,
                    scoresImproved, popnClassDelta, List.copyOf(unmatched));
        } catch (RuntimeException exception) {
            finishFailureLog(renewLogId, matched, exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @Override
    @Transactional
    public PopclassRecalculationResult recalculate(String poptomoId) {
        return rebuildPopclass(poptomoId, findUserId(poptomoId), null);
    }

    private PopclassRecalculationResult rebuildPopclass(
            String poptomoId, long userId, Integer requestedDisplayPopclass) {
        List<PopclassRow> rows = jdbc.query("""
                SELECT p.playdata_id, p.chart_id, p.current_version,
                       p.version_score, p.version_score_known, p.all_time_score,
                       p.medal_code, c.level, c.chart_version
                  FROM playdata p
                 JOIN charts c ON c.chart_id = p.chart_id
                 WHERE p.user_id = ? AND c.is_deleted = FALSE
                """, (rs, rowNum) -> new PopclassRow(
                rs.getLong("playdata_id"), rs.getLong("chart_id"),
                rs.getInt("current_version"),
                rs.getInt("version_score"), rs.getBoolean("version_score_known"),
                rs.getInt("all_time_score"),
                rs.getInt("medal_code"), rs.getInt("level"), rs.getInt("chart_version")),
                userId);

        for (PopclassRow row : rows) {
            row.displayPopclass = row.playdataVersion == currentVersion && row.versionScoreKnown
                    ? popclassPolicy.newChartPopclass(
                            row.level, row.versionScore, row.medalCode)
                    : 0;
            row.potentialPopclass = popclassPolicy.newChartPopclass(
                    row.level, row.allTimeScore, row.medalCode);
            row.legacyPopclass = popclassPolicy.legacyChartPopclass(
                    row.level, row.allTimeScore, row.medalCode);
            jdbc.update("""
                    UPDATE playdata
                       SET popclass = ?, is_display_popclass_target = FALSE,
                           popclass_bucket = NULL, popclass_bucket_rank = NULL
                     WHERE playdata_id = ?
                    """, row.displayPopclass, row.playdataId);
        }

        Comparator<PopclassRow> displayOrder = Comparator
                .comparingInt((PopclassRow row) -> row.displayPopclass).reversed()
                .thenComparing(Comparator.comparingInt(
                        (PopclassRow row) -> row.allTimeScore).reversed())
                .thenComparingLong(row -> row.chartId);
        List<PopclassRow> current = rows.stream()
                .filter(row -> row.chartVersion == currentVersion)
                .sorted(displayOrder).limit(20).toList();
        List<PopclassRow> old = rows.stream()
                .filter(row -> row.chartVersion != currentVersion)
                .sorted(displayOrder).limit(40).toList();
        mark(current, "CURRENT_VERSION");
        mark(old, "OLD_VERSION");

        int calculatedDisplayPopclass = popclassPolicy.newUserPopclassFromCharts(
                java.util.stream.Stream.concat(current.stream(), old.stream())
                        .map(row -> new PopclassPolicy.NewChartScore(
                                row.level,
                                row.playdataVersion == currentVersion && row.versionScoreKnown
                                        ? row.versionScore : 0,
                                row.medalCode)).toList());
        int displayPopclass = requestedDisplayPopclass == null
                ? calculatedDisplayPopclass : requestedDisplayPopclass;
        Comparator<PopclassRow> potentialOrder = Comparator.comparingInt(
                        (PopclassRow row) -> row.potentialPopclass).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (PopclassRow row) -> row.allTimeScore).reversed())
                        .thenComparingLong(row -> row.chartId);
        List<PopclassRow> potentialCurrent = rows.stream()
                .filter(row -> row.chartVersion == currentVersion)
                .sorted(potentialOrder).limit(20).toList();
        List<PopclassRow> potentialOld = rows.stream()
                .filter(row -> row.chartVersion != currentVersion)
                .sorted(potentialOrder).limit(40).toList();
        int potentialPopclass = popclassPolicy.newUserPopclassFromCharts(
                java.util.stream.Stream.concat(potentialCurrent.stream(), potentialOld.stream())
                        .map(row -> new PopclassPolicy.NewChartScore(
                                row.level, row.allTimeScore, row.medalCode)).toList());
        int legacyPopclass = popclassPolicy.legacyUserPopclass(rows.stream()
                .sorted(Comparator.comparingInt(
                        (PopclassRow row) -> row.legacyPopclass).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (PopclassRow row) -> row.allTimeScore).reversed())
                        .thenComparingLong(row -> row.chartId))
                .limit(50).map(row -> row.legacyPopclass).toList());
        jdbc.update("""
                UPDATE user_profiles
                   SET display_popclass = ?, potential_popclass = ?, legacy_popclass = ?,
                       updated_at = ?
                 WHERE user_id = ?
                """, displayPopclass, potentialPopclass, legacyPopclass,
                Timestamp.from(Instant.now()), userId);
        return new PopclassRecalculationResult(
                poptomoId, legacyPopclass, displayPopclass, potentialPopclass,
                PopclassPolicy.NEW_POPCLASS_SCALE, current.size(), old.size());
    }

    private void mark(List<PopclassRow> rows, String bucket) {
        for (int index = 0; index < rows.size(); index++) {
            jdbc.update("""
                    UPDATE playdata
                       SET is_display_popclass_target = TRUE,
                           popclass_bucket = ?, popclass_bucket_rank = ?
                     WHERE playdata_id = ?
                    """, bucket, index + 1, rows.get(index).playdataId);
        }
    }

    private UpsertOutcome upsert(long userId, long chartId, ImportPlaydataCommand.Row row,
                                 long renewLogId) {
        PlaydataUpsertPolicy.State existing = loadState(userId, chartId);
        var observed = new PlaydataUpsertPolicy.Observation(
                row.score(), row.rankCode(), row.medalCode(), row.versionBestScore(), row.versionBestScorePresent());
        var transition = existing == null || existing.currentVersion() == currentVersion
                ? null : loadTransition(existing.currentVersion(), currentVersion);
        var decision = upsertPolicy.decide(existing, observed, currentVersion, transition);
        boolean resetVersionKnowledge = existing == null
                || existing.currentVersion() != currentVersion;
        if (!decision.changed()) {
            updateVersionScoreKnowledge(userId, chartId, observed, resetVersionKnowledge);
            return new UpsertOutcome(false, 0, false, false, false);
        }
        if (existing == null) {
            insertState(userId, chartId, renewLogId, decision.state());
        } else {
            updateState(userId, chartId, renewLogId, decision.state());
        }
        var events = historyPolicy.events(existing, decision.state(), transition);
        appendHistory(userId, chartId, renewLogId, existing, decision.state(), events);
        updateVersionScoreKnowledge(userId, chartId, observed, resetVersionKnowledge);
        boolean recordAdded = existing == null;
        boolean medalImproved = existing != null
                && decision.state().medalCode() < existing.medalCode();
        boolean scoreImproved = existing != null
                && (decision.state().versionScore() > existing.versionScore()
                || decision.state().allTimeScore() > existing.allTimeScore());
        return new UpsertOutcome(true, events.size(), recordAdded, medalImproved, scoreImproved);
    }

    private void updateVersionScoreKnowledge(long userId, long chartId,
                                             PlaydataUpsertPolicy.Observation observed,
                                             boolean reset) {
        if (!observed.versionBestScorePresent() && !reset) return;
        jdbc.update("""
                UPDATE playdata SET version_score_known = ?
                 WHERE user_id = ? AND chart_id = ?
                """, observed.versionBestScorePresent(), userId, chartId);
    }

    private PlaydataUpsertPolicy.State loadState(long userId, long chartId) {
        List<PlaydataUpsertPolicy.State> states = jdbc.query("""
                SELECT current_version, version_score, version_rank_code,
                       all_time_score, all_time_score_version, all_time_rank_code, medal_code
                  FROM playdata WHERE user_id = ? AND chart_id = ?
                """, (rs, rowNum) -> new PlaydataUpsertPolicy.State(
                rs.getInt("current_version"), rs.getInt("version_score"),
                (Integer) rs.getObject("version_rank_code"), rs.getInt("all_time_score"),
                rs.getInt("all_time_score_version"), (Integer) rs.getObject("all_time_rank_code"),
                rs.getInt("medal_code")), userId, chartId);
        if (states.size() > 1) throw new IllegalStateException("Duplicate playdata current state.");
        return states.isEmpty() ? null : states.getFirst();
    }

    private PlaydataUpsertPolicy.TransitionPolicy loadTransition(int fromVersion, int toVersion) {
        List<String> policies = jdbc.query("""
                SELECT score_policy FROM game_version_transitions
                 WHERE from_version = ? AND to_version = ? AND status = 'APPROVED'
                """, (rs, rowNum) -> rs.getString(1), fromVersion, toVersion);
        if (policies.size() != 1) return null;
        return PlaydataUpsertPolicy.TransitionPolicy.fromDatabase(policies.getFirst());
    }

    private void insertState(long userId, long chartId, long renewLogId,
                             PlaydataUpsertPolicy.State state) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO playdata
                    (user_id, chart_id, current_version, version_score, version_rank_code,
                     all_time_score, all_time_score_version, all_time_rank_code, medal_code,
                     popclass, is_display_popclass_target, last_renew_log_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, FALSE, ?, ?, ?)
                """, userId, chartId, state.currentVersion(), state.versionScore(),
                state.versionRankCode(), state.allTimeScore(), state.allTimeScoreVersion(),
                state.allTimeRankCode(), state.medalCode(), renewLogId, now, now);
    }

    private void updateState(long userId, long chartId, long renewLogId,
                             PlaydataUpsertPolicy.State state) {
        jdbc.update("""
                UPDATE playdata
                   SET current_version = ?, version_score = ?, version_rank_code = ?,
                       all_time_score = ?, all_time_score_version = ?, all_time_rank_code = ?,
                       medal_code = ?, last_renew_log_id = ?, updated_at = ?
                 WHERE user_id = ? AND chart_id = ?
                """, state.currentVersion(), state.versionScore(), state.versionRankCode(),
                state.allTimeScore(), state.allTimeScoreVersion(), state.allTimeRankCode(),
                state.medalCode(), renewLogId, Timestamp.from(Instant.now()), userId, chartId);
    }

    private void appendHistory(long userId, long chartId, long renewLogId,
                               PlaydataUpsertPolicy.State previous,
                               PlaydataUpsertPolicy.State current,
                               List<PlaydataHistoryPolicy.EventType> events) {
        for (var event : events) {
            jdbc.update("""
                    INSERT INTO playdata_history
                        (user_id, chart_id, game_version,
                         previous_version_score, version_score,
                         previous_all_time_score, all_time_score,
                         previous_rank_code, rank_code,
                         previous_medal_code, medal_code,
                         popclass, event_type, renew_log_id, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
                    """, userId, chartId, current.currentVersion(),
                    previous == null ? null : previous.versionScore(), current.versionScore(),
                    previous == null ? null : previous.allTimeScore(), current.allTimeScore(),
                    previous == null ? null : previous.versionRankCode(), current.versionRankCode(),
                    previous == null ? null : previous.medalCode(), current.medalCode(),
                    event.name(), renewLogId, Timestamp.from(Instant.now()));
        }
    }

    private long findUserId(String poptomoId) {
        List<Long> ids = jdbc.query("SELECT user_id FROM users WHERE poptomo_id = ?",
                (rs, rowNum) -> rs.getLong(1), poptomoId);
        if (ids.size() != 1) {
            throw new IllegalArgumentException("Authenticated user was not found.");
        }
        return ids.getFirst();
    }

    private Integer findDisplayPopclass(long userId) {
        List<Integer> values = jdbc.query(
                "SELECT display_popclass FROM user_profiles WHERE user_id = ?",
                (rs, rowNum) -> (Integer) rs.getObject(1), userId);
        return values.isEmpty() ? null : values.getFirst();
    }

    private void updateProfile(long userId, ImportPlaydataCommand.ProfileSnapshot profile) {
        if (profile == null) return;
        jdbc.update("""
                UPDATE user_profiles
                   SET user_name = COALESCE(?, user_name),
                       character_name = COALESCE(?, character_name),
                       normal_credit = COALESCE(?, normal_credit),
                       extra_credit = COALESCE(?, extra_credit),
                       time_play_10_credit = COALESCE(?, time_play_10_credit),
                       time_play_16_credit = COALESCE(?, time_play_16_credit),
                       updated_at = ?
                 WHERE user_id = ?
                """, profile.userName(), profile.characterName(), profile.normalCredit(),
                profile.extraCredit(), profile.timePlay10Credit(), profile.timePlay16Credit(),
                Timestamp.from(Instant.now()), userId);
    }

    private long startLog(ImportPlaydataCommand command, long userId) {
        if (independentTransaction == null) {
            return insertLog(command, userId);
        }
        Long id = independentTransaction.execute(status -> insertLog(command, userId));
        if (id == null) {
            throw new IllegalStateException("Could not create renew log.");
        }
        return id;
    }

    private long insertLog(ImportPlaydataCommand command, long userId) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO renew_logs
                        (poptomo_id, user_id, status, mode, input_chart_count,
                         matched_chart_count, updated_playdata_count, failure_reason, ip, created_at)
                    VALUES (?, ?, 'RUNNING', 'IMPORT', ?, 0, 0, NULL, NULL, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, command.poptomoId());
            statement.setLong(2, userId);
            statement.setInt(3, command.rows().size());
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            return statement;
        }, keys);
        if (keys.getKey() == null) throw new IllegalStateException("renew log id was not generated.");
        return keys.getKey().longValue();
    }

    private Match match(ImportPlaydataCommand.Row row) {
        if (row.chartId() != null) {
            return unique("SELECT chart_id FROM charts WHERE chart_id = ? AND is_deleted = FALSE",
                    row.chartId());
        }
        if (row.songId() != null) {
            return unique("""
                    SELECT chart_id FROM charts
                     WHERE song_id = ? AND difficulty_code = ? AND is_upper = ? AND is_deleted = FALSE
                    """, row.songId(), row.difficultyCode(), row.upper());
        }
        if (row.songHash() != null && !row.songHash().isBlank()) {
            return unique("""
                    SELECT c.chart_id FROM charts c JOIN songs s ON s.song_id = c.song_id
                     WHERE s.song_hash = ? AND c.difficulty_code = ? AND c.is_upper = ?
                       AND c.is_deleted = FALSE
                    """, row.songHash(), row.difficultyCode(), row.upper());
        }
        if (row.artistName() != null && !row.artistName().isBlank()) {
            return unique("""
                    SELECT c.chart_id FROM charts c JOIN songs s ON s.song_id = c.song_id
                     WHERE s.song_name = ? AND s.genre_name = ? AND s.artist_name = ?
                       AND c.difficulty_code = ? AND c.is_upper = ? AND c.is_deleted = FALSE
                    """, row.songName(), row.genreName(), row.artistName(),
                    row.difficultyCode(), row.upper());
        }
        return unique("""
                SELECT c.chart_id FROM charts c JOIN songs s ON s.song_id = c.song_id
                 WHERE s.song_name = ? AND s.genre_name = ?
                   AND c.difficulty_code = ? AND c.is_upper = ? AND c.is_deleted = FALSE
                """, row.songName(), row.genreName(), row.difficultyCode(), row.upper());
    }

    private Match unique(String sql, Object... args) {
        List<Long> ids = jdbc.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        if (ids.isEmpty()) return new Match(null, "CHART_NOT_FOUND");
        if (ids.size() > 1) return new Match(null, "AMBIGUOUS_CHART");
        return new Match(ids.getFirst(), null);
    }

    private void finishLog(long id, String status, int matched, int updated, String reason) {
        jdbc.update("""
                UPDATE renew_logs SET status = ?, matched_chart_count = ?,
                       updated_playdata_count = ?, failure_reason = ?
                 WHERE renew_log_id = ?
                """, status, matched, updated, reason, id);
    }

    private void finishFailureLog(long id, int matched, String reason) {
        if (independentTransaction == null) {
            finishLog(id, "FAILED", matched, 0, reason);
            return;
        }
        independentTransaction.executeWithoutResult(
                status -> finishLog(id, "FAILED", matched, 0, reason));
    }

    private static String summarize(List<ImportPlaydataResult.UnmatchedRow> rows) {
        long notFound = rows.stream().filter(row -> row.reason().equals("CHART_NOT_FOUND")).count();
        long ambiguous = rows.size() - notFound;
        return "CHART_NOT_FOUND=" + notFound + ",AMBIGUOUS_CHART=" + ambiguous;
    }

    private record Match(Long chartId, String reason) {
    }

    private record UpsertOutcome(boolean updated, int historyCount, boolean recordAdded,
                                 boolean medalImproved, boolean scoreImproved) {
    }

    private static final class PopclassRow {
        private final long playdataId;
        private final long chartId;
        private final int playdataVersion;
        private final int versionScore;
        private final boolean versionScoreKnown;
        private final int allTimeScore;
        private final int medalCode;
        private final int level;
        private final int chartVersion;
        private int displayPopclass;
        private int potentialPopclass;
        private int legacyPopclass;

        private PopclassRow(long playdataId, long chartId, int playdataVersion,
                            int versionScore, boolean versionScoreKnown,
                            int allTimeScore, int medalCode,
                            int level, int chartVersion) {
            this.playdataId = playdataId;
            this.chartId = chartId;
            this.playdataVersion = playdataVersion;
            this.versionScore = versionScore;
            this.versionScoreKnown = versionScoreKnown;
            this.allTimeScore = allTimeScore;
            this.medalCode = medalCode;
            this.level = level;
            this.chartVersion = chartVersion;
        }
    }
}
