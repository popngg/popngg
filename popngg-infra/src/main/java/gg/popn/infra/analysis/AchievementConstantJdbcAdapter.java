package gg.popn.infra.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AchievementConstants;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Component
public class AchievementConstantJdbcAdapter implements AchievementConstants {
    private static final Set<String> MEDAL_TARGETS = Set.of(
            "CLEAR", "BRONZE_DIAMOND", "BRONZE_STAR", "FULL_COMBO", "PERFECT");
    private static final Set<String> RANK_TARGETS = Set.of("AA", "AA_PLUS", "AAA", "S", "S_PLUS");
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public AchievementConstantJdbcAdapter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Snapshot latest(int level, Axis axis) {
        if (level < 48 || level > 50) throw new IllegalArgumentException("Unsupported level");
        var headers = jdbc.query("""
                SELECT snapshot_id, source_snapshot_id, model_version, model_status, generated_at
                  FROM achievement_constant_snapshots WHERE active_axis = ?
                """, (rs, row) -> new Header(rs.getLong("snapshot_id"), rs.getString("source_snapshot_id"),
                rs.getString("model_version"), rs.getString("model_status"),
                rs.getTimestamp("generated_at").toInstant()), axis.name());
        if (headers.isEmpty()) throw new IllegalStateException("ACHIEVEMENT_CONSTANTS_NOT_READY");
        Header header = headers.getFirst();
        var grouped = new LinkedHashMap<Long, MutableChart>();
        jdbc.query("""
                SELECT ac.chart_id, ac.target, ac.raw_difficulty, ac.constant_value,
                       ac.lower_bound, ac.upper_bound, ac.player_count, ac.achieved_count,
                       ac.status, ac.hold_reasons, s.song_name, s.genre_name, s.jacket_url,
                       c.level, c.difficulty_code, c.is_upper,
                       COALESCE(csf.has_strict_gauge, c.has_strict_gauge) strict_gauge,
                       COALESCE(csf.has_strict_judgement, c.has_strict_judgement) strict_judgement,
                       COALESCE(csf.extra_type, 'NONE') extra_type
                  FROM achievement_constants ac
                  JOIN charts c ON c.chart_id = ac.chart_id AND c.is_deleted = FALSE
                  JOIN songs s ON s.song_id = c.song_id
                  LEFT JOIN chart_special_flags csf ON csf.chart_id = c.chart_id
                 WHERE ac.snapshot_id = ? AND c.level = ?
                 ORDER BY ac.chart_id, ac.target
                """, rs -> {
            long chartId = rs.getLong("chart_id");
            var chart = grouped.get(chartId);
            if (chart == null) {
                chart = mutableChart(rs);
                grouped.put(chartId, chart);
            }
            chart.constants.add(new Constant(rs.getString("target"), nullableDouble(rs, "raw_difficulty"),
                    nullableDouble(rs, "constant_value"), nullableDouble(rs, "lower_bound"),
                    nullableDouble(rs, "upper_bound"), rs.getInt("player_count"),
                    rs.getInt("achieved_count"), rs.getString("status"), reasons(rs.getString("hold_reasons"))));
        }, header.id, level);
        var charts = grouped.values().stream().map(MutableChart::freeze).toList();
        return new Snapshot(header.id, header.sourceId, header.modelVersion, header.modelStatus,
                header.generatedAt, axis, level, charts);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateImport(Import request) { validate(request); }

    @Override
    @Transactional
    public Stored importSnapshot(Import request) {
        validate(request);
        String payloadHash = payloadHash(request);
        jdbc.queryForObject("SELECT axis FROM achievement_constant_guards WHERE axis = ? FOR UPDATE",
                String.class, request.axis().name());
        var existing = jdbc.query("""
                SELECT snapshot_id, payload_sha256 FROM achievement_constant_snapshots
                 WHERE source_snapshot_id = ? AND axis = ?
                """, (rs, row) -> new Existing(rs.getLong(1), rs.getString(2)),
                request.sourceSnapshotId(), request.axis().name());
        if (!existing.isEmpty()) {
            if (!payloadHash.equals(existing.getFirst().hash))
                throw new IllegalArgumentException("Source snapshot content changed");
            jdbc.update("UPDATE achievement_constant_snapshots SET active_axis = NULL WHERE active_axis = ?",
                    request.axis().name());
            jdbc.update("UPDATE achievement_constant_snapshots SET active_axis = ? WHERE snapshot_id = ?",
                    request.axis().name(), existing.getFirst().id);
            return new Stored(existing.getFirst().id, request.sourceSnapshotId(), request.axis(), request.constants().size());
        }
        jdbc.update("""
                INSERT INTO achievement_constant_snapshots
                    (source_snapshot_id, axis, model_version, model_status, payload_sha256, generated_at, row_count)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, request.sourceSnapshotId(), request.axis().name(), request.modelVersion(),
                request.modelStatus(), payloadHash, Timestamp.from(request.generatedAt()), request.constants().size());
        long snapshotId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (var row : request.constants()) {
            Double lower = row.interval() == null || row.interval().size() != 2 ? null : row.interval().get(0);
            Double upper = row.interval() == null || row.interval().size() != 2 ? null : row.interval().get(1);
            jdbc.update("""
                    INSERT INTO achievement_constants
                        (snapshot_id, chart_id, target, raw_difficulty, constant_value, lower_bound,
                         upper_bound, player_count, achieved_count, status, hold_reasons)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                    """, snapshotId, row.chartId(), row.target(), row.rawDifficulty(), row.difficultyConstant(),
                    lower, upper, row.playerCount(), row.achievedCount(), row.status(), json(row.holdReasons()));
        }
        jdbc.update("UPDATE achievement_constant_snapshots SET active_axis = NULL WHERE active_axis = ?",
                request.axis().name());
        jdbc.update("UPDATE achievement_constant_snapshots SET active_axis = ? WHERE snapshot_id = ?",
                request.axis().name(), snapshotId);
        return new Stored(snapshotId, request.sourceSnapshotId(), request.axis(), request.constants().size());
    }

    private void validate(Import request) {
        if (request == null || request.axis() == null || request.generatedAt() == null
                || request.sourceSnapshotId() == null || !request.sourceSnapshotId().matches("[A-Za-z0-9_./:-]{1,160}")
                || request.modelVersion() == null || !request.modelVersion().matches("[A-Za-z0-9_.-]{1,64}")
                || !"EXPERIMENTAL".equals(request.modelStatus()) || request.constants() == null
                || request.constants().isEmpty() || request.constants().size() > 5000)
            throw new IllegalArgumentException("Invalid achievement snapshot");
        Set<String> targets = request.axis() == Axis.MEDAL ? MEDAL_TARGETS : RANK_TARGETS;
        var keys = new HashSet<String>();
        for (var row : request.constants()) {
            if (row == null || row.chartId() <= 0 || row.level() < 48 || row.level() > 50
                    || !request.axis().name().equalsIgnoreCase(row.axis())
                    || row.target() == null || !targets.contains(row.target()) || row.playerCount() < 0
                    || row.achievedCount() < 0 || row.achievedCount() > row.playerCount()
                    || !("EXPERIMENTAL".equals(row.status()) || "HOLD".equals(row.status()))
                    || !keys.add(row.chartId() + ":" + row.target()) || !validResult(row))
                throw new IllegalArgumentException("Invalid achievement constant row");
        }
        var ids = request.constants().stream().map(ImportRow::chartId).distinct().toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        var levels = new HashMap<Long,Integer>();
        jdbc.query("SELECT chart_id, level FROM charts WHERE is_deleted = FALSE AND chart_id IN ("
                + placeholders + ")", (org.springframework.jdbc.core.RowCallbackHandler)
                rs -> levels.put(rs.getLong(1), rs.getInt(2)), ids.toArray());
        if (levels.size() != ids.size() || request.constants().stream()
                .anyMatch(row -> !Objects.equals(levels.get(row.chartId()), row.level())))
            throw new IllegalArgumentException("Unknown chart or level mismatch in snapshot");
        var hashes = jdbc.query("SELECT payload_sha256 FROM achievement_constant_snapshots WHERE source_snapshot_id=? AND axis=?",
                (rs,row)->rs.getString(1), request.sourceSnapshotId(), request.axis().name());
        if (!hashes.isEmpty() && !hashes.getFirst().equals(payloadHash(request)))
            throw new IllegalArgumentException("Source snapshot content changed");
    }

    private static boolean validResult(ImportRow row) {
        if (row.rawDifficulty() != null && !Double.isFinite(row.rawDifficulty())) return false;
        if (row.holdReasons() != null && row.holdReasons().stream()
                .anyMatch(reason -> reason == null || reason.isBlank())) return false;
        if ("HOLD".equals(row.status())) {
            return row.difficultyConstant() == null && row.interval() == null
                    && row.holdReasons() != null && !row.holdReasons().isEmpty();
        }
        return finite(row.rawDifficulty()) && finite(row.difficultyConstant())
                && row.interval() != null && row.interval().size() == 2
                && finite(row.interval().get(0)) && finite(row.interval().get(1))
                && row.interval().get(0) <= row.interval().get(1)
                && (row.holdReasons() == null || row.holdReasons().isEmpty());
    }

    private static boolean finite(Double value) { return value != null && Double.isFinite(value); }

    private static Double nullableDouble(ResultSet rs, String column) throws java.sql.SQLException {
        Object value = rs.getObject(column); return value == null ? null : ((Number)value).doubleValue();
    }
    private MutableChart mutableChart(ResultSet rs) throws java.sql.SQLException {
        return new MutableChart(rs.getLong("chart_id"), rs.getString("song_name"), rs.getString("genre_name"),
                rs.getString("jacket_url"), rs.getInt("level"), rs.getInt("difficulty_code"),
                rs.getBoolean("is_upper"), rs.getBoolean("strict_gauge"), rs.getBoolean("strict_judgement"),
                rs.getString("extra_type"));
    }
    private List<String> reasons(String value) {
        try { return mapper.readValue(value, new TypeReference<>() {}); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Invalid stored hold reasons", exception); }
    }
    private String json(List<String> value) {
        try { return mapper.writeValueAsString(value == null ? List.of() : value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Invalid hold reasons", exception); }
    }
    private String payloadHash(Import request) {
        try {
            byte[] bytes=mapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid achievement snapshot", exception);
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private record Existing(long id,String hash) {}
    private record Header(long id, String sourceId, String modelVersion, String modelStatus, Instant generatedAt) {}
    private static final class MutableChart {
        final long id; final String songName, genreName, jacketUrl; final int level, difficulty;
        final boolean upper, strictGauge, strictJudgement; final String extraType;
        final List<Constant> constants = new ArrayList<>();
        MutableChart(long id, String songName, String genreName, String jacketUrl, int level, int difficulty,
                     boolean upper, boolean strictGauge, boolean strictJudgement, String extraType) {
            this.id=id; this.songName=songName; this.genreName=genreName; this.jacketUrl=jacketUrl;
            this.level=level; this.difficulty=difficulty; this.upper=upper; this.strictGauge=strictGauge;
            this.strictJudgement=strictJudgement; this.extraType=extraType;
        }
        Chart freeze() { return new Chart(id, songName, genreName, jacketUrl, level, difficulty, upper,
                strictGauge, strictJudgement, extraType, List.copyOf(constants)); }
    }
}
