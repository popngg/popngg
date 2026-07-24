package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.dto.result.ImportPlaydataResult;
import gg.popn.application.playdata.port.out.PlaydataImportPort;
import gg.popn.application.playdata.service.PlaydataUpsertPolicy;
import gg.popn.application.playdata.service.PlaydataHistoryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlaydataImportJdbcAdapter implements PlaydataImportPort {
    private final JdbcTemplate jdbc;
    private final PlaydataUpsertPolicy upsertPolicy;
    private final PlaydataHistoryPolicy historyPolicy;
    private final int currentVersion;

    public PlaydataImportJdbcAdapter(JdbcTemplate jdbc, PlaydataUpsertPolicy upsertPolicy,
                                     PlaydataHistoryPolicy historyPolicy,
                                     @Value("${popngg.game.current-version:29}") int currentVersion) {
        this.jdbc = jdbc;
        this.upsertPolicy = upsertPolicy;
        this.historyPolicy = historyPolicy;
        this.currentVersion = currentVersion;
    }

    @Override
    @Transactional
    public ImportPlaydataResult execute(ImportPlaydataCommand command) {
        long userId = findUserId(command.poptomoId());
        updateProfile(userId, command.profile());
        long renewLogId = startLog(command, userId);
        var unmatched = new ArrayList<ImportPlaydataResult.UnmatchedRow>();
        int matched = 0;
        int updated = 0;
        int histories = 0;
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
                }
            }
            String status = unmatched.isEmpty() ? "SUCCESS" : matched == 0 ? "FAILED" : "PARTIAL_SUCCESS";
            finishLog(renewLogId, status, matched, updated,
                    unmatched.isEmpty() ? null : summarize(unmatched));
            return new ImportPlaydataResult(renewLogId, command.rows().size(), matched,
                    updated, histories, unmatched.size(), List.copyOf(unmatched));
        } catch (RuntimeException exception) {
            finishLog(renewLogId, "FAILED", matched, 0, exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private UpsertOutcome upsert(long userId, long chartId, ImportPlaydataCommand.Row row,
                                 long renewLogId) {
        PlaydataUpsertPolicy.State existing = loadState(userId, chartId);
        var observed = new PlaydataUpsertPolicy.Observation(
                row.score(), row.rankCode(), row.medalCode());
        var transition = existing == null || existing.currentVersion() == currentVersion
                ? null : loadTransition(existing.currentVersion(), currentVersion);
        var decision = upsertPolicy.decide(existing, observed, currentVersion, transition);
        if (!decision.changed()) return new UpsertOutcome(false, 0);
        if (existing == null) {
            insertState(userId, chartId, renewLogId, decision.state());
        } else {
            updateState(userId, chartId, renewLogId, decision.state());
        }
        var events = historyPolicy.events(existing, decision.state(), transition);
        appendHistory(userId, chartId, renewLogId, existing, decision.state(), events);
        return new UpsertOutcome(true, events.size());
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

    private static String summarize(List<ImportPlaydataResult.UnmatchedRow> rows) {
        long notFound = rows.stream().filter(row -> row.reason().equals("CHART_NOT_FOUND")).count();
        long ambiguous = rows.size() - notFound;
        return "CHART_NOT_FOUND=" + notFound + ",AMBIGUOUS_CHART=" + ambiguous;
    }

    private record Match(Long chartId, String reason) {
    }

    private record UpsertOutcome(boolean updated, int historyCount) {
    }
}
