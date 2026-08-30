package gg.popn.infra.db.adapter;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.dto.query.FindUserRecordsQuery;
import gg.popn.application.playdata.exception.ActualPopclassUnavailableException;
import gg.popn.application.playdata.port.out.PlaydataQueryPort;
import gg.popn.application.playdata.service.PopclassPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class PlaydataQueryJdbcAdapter implements PlaydataQueryPort {
    private final JdbcTemplate jdbc;
    private final int currentVersion;
    private final PopclassPolicy popclassPolicy;

    @Autowired
    public PlaydataQueryJdbcAdapter(
            JdbcTemplate jdbc,
            @Value("${popngg.game.current-version:29}") int currentVersion,
            PopclassPolicy popclassPolicy
    ) {
        this.jdbc = jdbc;
        this.currentVersion = currentVersion;
        this.popclassPolicy = popclassPolicy;
    }

    public PlaydataQueryJdbcAdapter(JdbcTemplate jdbc, int currentVersion) {
        this(jdbc, currentVersion, new PopclassPolicy());
    }

    @Override
    public PlaydataQueryResults.UserPlaydata findUserPlaydata(String poptomoId) {
        UserSummary user = findUser(poptomoId);
        List<PlaydataQueryResults.ChartPlaydata> rows = queryPlaydata("""
                SELECT p.*, c.level, c.difficulty_code, c.difficulty_label,
                       c.chart_version, c.is_upper,
                       s.song_hash, s.genre_name, s.song_name
                  FROM playdata p
                  JOIN charts c ON c.chart_id = p.chart_id
                  JOIN songs s ON s.song_id = c.song_id
                 WHERE p.user_id = ? AND p.current_version = ? AND c.is_deleted = FALSE
                 ORDER BY p.chart_id
                """, user.userId(), currentVersion);
        return new PlaydataQueryResults.UserPlaydata(
                poptomoId, user.userName(), user.displayPopclass(),
                user.potentialPopclass(), user.legacyPopclass(), rows);
    }

    @Override
    public PlaydataQueryResults.Counts count(String poptomoId, String groupBy, String target) {
        UserSummary user = findUser(poptomoId);
        String groupColumn = groupBy.equals("LEVEL") ? "c.level" : "c.difficulty_code";
        String groupLabel = groupBy.equals("LEVEL") ? "CAST(c.level AS CHAR)" : "c.difficulty_label";
        String targetColumn = target.equals("RANK") ? "p.version_rank_code" : "p.medal_code";
        String sql = """
                SELECT %s AS group_code, %s AS group_label,
                       %s AS target_code, COUNT(*) AS row_count
                  FROM playdata p
                  JOIN charts c ON c.chart_id = p.chart_id
                 WHERE p.user_id = ? AND p.current_version = ? AND c.is_deleted = FALSE
                   AND %s IS NOT NULL
                 GROUP BY %s, %s, %s
                 ORDER BY %s, %s
                """.formatted(groupColumn, groupLabel, targetColumn, targetColumn,
                groupColumn, groupLabel, targetColumn, groupColumn, targetColumn);
        List<PlaydataQueryResults.GroupCount> groups = jdbc.query(sql, (rs, rowNum) ->
                        new PlaydataQueryResults.GroupCount(
                                groupBy, rs.getInt("group_code"), rs.getString("group_label"),
                                target, rs.getInt("target_code"), rs.getLong("row_count")),
                user.userId(), currentVersion);
        return new PlaydataQueryResults.Counts(groups);
    }

    @Override
    public PlaydataQueryResults.UserRecords findUserRecords(
            String poptomoId, FindUserRecordsQuery query) {
        UserSummary user = findUser(poptomoId);
        var where = new StringBuilder("""
                 FROM playdata p
                 JOIN charts c ON c.chart_id = p.chart_id
                 JOIN songs s ON s.song_id = c.song_id
                WHERE p.user_id = ? AND p.current_version = ? AND c.is_deleted = FALSE
                """);
        List<Object> args = new ArrayList<>();
        args.add(user.userId());
        args.add(currentVersion);
        addLike(where, args, query.keyword());
        addEquals(where, args, "s.version", query.version());
        addBound(where, args, "c.level", ">=", query.levelMin());
        addBound(where, args, "c.level", "<=", query.levelMax());
        addIn(where, args, "c.difficulty_code", query.difficulties());
        addIn(where, args, "p.medal_code", query.medals());
        addIn(where, args, "COALESCE(p.all_time_rank_code, 13)", query.ranks());
        addBound(where, args, "p.all_time_score", ">=", query.scoreMin());
        addBound(where, args, "p.all_time_score", "<=", query.scoreMax());

        long total = jdbc.queryForObject("SELECT COUNT(*) " + where, Long.class, args.toArray());
        String sortColumn = switch (query.sort()) {
            case "LEVEL" -> "c.level";
            case "VERSION" -> "s.version";
            case "DIFFICULTY" -> "c.difficulty_code";
            case "TITLE" -> "s.song_name";
            case "GENRE" -> "s.genre_name";
            case "SCORE" -> "p.all_time_score";
            case "MEDAL" -> "p.medal_code";
            case "RANK" -> "COALESCE(p.all_time_rank_code, 13)";
            case "POPCLASS" -> "p.popclass";
            default -> throw new IllegalArgumentException("Unsupported record sort.");
        };
        String sql = """
                SELECT s.song_hash, s.song_name, s.genre_name, s.jacket_url, s.version,
                       c.difficulty_code, c.level, p.all_time_score, p.medal_code,
                       COALESCE(p.all_time_rank_code, 13) AS rank_code, p.popclass
                """ + where + " ORDER BY " + sortColumn + " " + query.order()
                + ", p.chart_id ASC LIMIT ? OFFSET ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(query.size());
        pageArgs.add(query.page() * query.size());
        var items = jdbc.query(sql, (rs, rowNum) -> new PlaydataQueryResults.UserRecord(
                required(rs.getString("song_hash")), rs.getString("song_name"),
                rs.getString("genre_name"), required(rs.getString("jacket_url")),
                rs.getInt("difficulty_code"), rs.getInt("level"),
                rs.getInt("all_time_score"), rs.getInt("medal_code"),
                rs.getInt("rank_code"), rs.getInt("version"), rs.getInt("popclass")),
                pageArgs.toArray());
        return new PlaydataQueryResults.UserRecords(
                items, total, query.page(), query.size());
    }

    @Override
    public PlaydataQueryResults.Progress findProgress(String poptomoId, String by) {
        UserSummary user = findUser(poptomoId);
        String groupColumn = by.equals("LEVEL") ? "c.level" : "c.difficulty_code";
        String sql = """
                SELECT %s AS group_code, p.all_time_score, p.medal_code,
                       COALESCE(p.all_time_rank_code, 13) AS rank_code
                  FROM playdata p
                  JOIN charts c ON c.chart_id = p.chart_id
                 WHERE p.user_id = ? AND p.current_version = ? AND c.is_deleted = FALSE
                 ORDER BY %s
                """.formatted(groupColumn, groupColumn);
        Map<Integer, ProgressAccumulator> rows = new LinkedHashMap<>();
        ProgressAccumulator summary = new ProgressAccumulator();
        jdbc.query(sql, rs -> {
            int key = rs.getInt("group_code");
            int score = rs.getInt("all_time_score");
            int medal = rs.getInt("medal_code");
            int rank = rs.getInt("rank_code");
            rows.computeIfAbsent(key, ignored -> new ProgressAccumulator())
                    .add(score, medal, rank);
            summary.add(score, medal, rank);
        }, user.userId(), currentVersion);
        var resultRows = rows.entrySet().stream()
                .map(entry -> entry.getValue().toRow(entry.getKey()))
                .toList();
        return new PlaydataQueryResults.Progress(resultRows, summary.toCounts());
    }

    private static void addLike(StringBuilder where, List<Object> args, String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        where.append(" AND (s.song_name LIKE ? OR s.genre_name LIKE ? OR s.artist_name LIKE ?)");
        String value = "%" + keyword.trim() + "%";
        args.add(value);
        args.add(value);
        args.add(value);
    }

    private static void addEquals(StringBuilder where, List<Object> args,
                                  String column, Integer value) {
        if (value == null) return;
        where.append(" AND ").append(column).append(" = ?");
        args.add(value);
    }

    private static void addBound(StringBuilder where, List<Object> args,
                                 String column, String operator, Integer value) {
        if (value == null) return;
        where.append(" AND ").append(column).append(' ').append(operator).append(" ?");
        args.add(value);
    }

    private static void addIn(StringBuilder where, List<Object> args,
                              String column, List<Integer> values) {
        if (values == null || values.isEmpty()) return;
        where.append(" AND ").append(column).append(" IN (")
                .append("?,".repeat(values.size()), 0, values.size() * 2 - 1)
                .append(')');
        args.addAll(values);
    }

    private static String required(String value) {
        return value == null ? "" : value;
    }

    private static final class ProgressAccumulator {
        private int total;
        private long scoreSum;
        private final Map<Integer, Integer> medals = new java.util.TreeMap<>();
        private final Map<Integer, Integer> ranks = new java.util.TreeMap<>();

        void add(int score, int medal, int rank) {
            total++;
            scoreSum += score;
            medals.merge(medal, 1, Integer::sum);
            ranks.merge(rank, 1, Integer::sum);
        }

        PlaydataQueryResults.ProgressRow toRow(int key) {
            return new PlaydataQueryResults.ProgressRow(
                    key, total, average(), codeCounts(medals), codeCounts(ranks));
        }

        PlaydataQueryResults.ProgressCounts toCounts() {
            return new PlaydataQueryResults.ProgressCounts(
                    total, average(), codeCounts(medals), codeCounts(ranks));
        }

        private int average() {
            return total == 0 ? 0 : (int) Math.round((double) scoreSum / total);
        }

        private static List<PlaydataQueryResults.CodeCount> codeCounts(
                Map<Integer, Integer> values) {
            return values.entrySet().stream()
                    .map(entry -> new PlaydataQueryResults.CodeCount(
                            entry.getKey(), entry.getValue()))
                    .toList();
        }
    }

    @Override
    public PlaydataQueryResults.Popclass findPopclass(String poptomoId) {
        UserSummary user = findUser(poptomoId);
        Integer total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM playdata p
                 JOIN charts c ON c.chart_id = p.chart_id
                 WHERE p.user_id = ? AND p.current_version = ? AND c.is_deleted = FALSE
                   AND p.is_display_popclass_target = TRUE
                """, Integer.class, user.userId(), currentVersion);
        Integer unknown = jdbc.queryForObject("""
                SELECT COUNT(*) FROM playdata p
                  JOIN charts c ON c.chart_id = p.chart_id
                 WHERE p.user_id = ? AND p.current_version = ? AND c.is_deleted = FALSE
                   AND p.is_display_popclass_target = TRUE
                   AND p.version_score_known = FALSE
                """, Integer.class, user.userId(), currentVersion);
        if (total == null || total == 0 || unknown == null || unknown > 0) {
            throw new ActualPopclassUnavailableException(poptomoId);
        }
        List<PlaydataQueryResults.ChartPlaydata> rows = queryPlaydata("""
                SELECT p.*, c.level, c.difficulty_code, c.difficulty_label,
                       c.chart_version, c.is_upper,
                       s.song_hash, s.genre_name, s.song_name
                  FROM playdata p
                  JOIN charts c ON c.chart_id = p.chart_id
                  JOIN songs s ON s.song_id = c.song_id
                 WHERE p.user_id = ? AND p.current_version = ?
                   AND p.is_display_popclass_target = TRUE AND c.is_deleted = FALSE
                 ORDER BY CASE p.popclass_bucket
                            WHEN 'CURRENT_VERSION' THEN 0 ELSE 1 END,
                          p.popclass_bucket_rank
                """, user.userId(), currentVersion);
        return new PlaydataQueryResults.Popclass(
                poptomoId, user.userName(), user.displayPopclass(),
                user.potentialPopclass(), user.legacyPopclass(), rows);
    }

    @Override
    public PlaydataQueryResults.Popclass findPotentialPopclass(String poptomoId) {
        UserSummary user = findUser(poptomoId);
        List<PlaydataQueryResults.ChartPlaydata> rows = queryPlaydata("""
                SELECT p.*, c.level, c.difficulty_code, c.difficulty_label,
                       c.chart_version, c.is_upper,
                       s.song_hash, s.genre_name, s.song_name
                  FROM playdata p
                  JOIN charts c ON c.chart_id = p.chart_id
                  JOIN songs s ON s.song_id = c.song_id
                 WHERE p.user_id = ? AND p.current_version = ? AND c.is_deleted = FALSE
                """, user.userId(), currentVersion).stream()
                .map(this::withPotentialPopclass)
                .toList();
        Comparator<PlaydataQueryResults.ChartPlaydata> order = Comparator
                .comparingInt((PlaydataQueryResults.ChartPlaydata row) -> row.popclass()).reversed()
                .thenComparing(Comparator.comparingInt(
                        (PlaydataQueryResults.ChartPlaydata row) -> row.allTimeBest().score()).reversed())
                .thenComparingLong(PlaydataQueryResults.ChartPlaydata::chartId);
        List<PlaydataQueryResults.ChartPlaydata> current = rankedBucket(rows.stream()
                .filter(row -> row.chartVersion() == currentVersion)
                .sorted(order).limit(20).toList(), "CURRENT_VERSION");
        List<PlaydataQueryResults.ChartPlaydata> old = rankedBucket(rows.stream()
                .filter(row -> row.chartVersion() != currentVersion)
                .sorted(order).limit(40).toList(), "OLD_VERSION");
        return new PlaydataQueryResults.Popclass(
                poptomoId, user.userName(), user.displayPopclass(),
                user.potentialPopclass(), user.legacyPopclass(),
                java.util.stream.Stream.concat(current.stream(), old.stream()).toList());
    }

    @Override
    public List<PlaydataQueryResults.ChartPlaydata> findLegacyPopclassTargets(
            String poptomoId) {
        UserSummary user = findUser(poptomoId);
        return queryPlaydata("""
                SELECT p.*, c.level, c.difficulty_code, c.difficulty_label,
                       c.chart_version, c.is_upper,
                       s.song_hash, s.genre_name, s.song_name
                  FROM playdata p
                  JOIN charts c ON c.chart_id = p.chart_id
                  JOIN songs s ON s.song_id = c.song_id
                 WHERE p.user_id = ? AND c.is_deleted = FALSE
                """, user.userId()).stream()
                .map(this::withLegacyPopclass)
                .sorted(Comparator
                        .comparingInt((PlaydataQueryResults.ChartPlaydata row) ->
                                row.popclass()).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (PlaydataQueryResults.ChartPlaydata row) ->
                                        row.allTimeBest().score()).reversed())
                        .thenComparingLong(PlaydataQueryResults.ChartPlaydata::chartId))
                .limit(50)
                .toList();
    }

    @Override
    public PlaydataQueryResults.ChartRankings findChartRankings(long chartId, int limit) {
        requireChart(chartId);
        List<PlaydataQueryResults.RankingEntry> current = queryRankings("""
                SELECT ranked.score, ranked.rank_code, ranked.medal_code,
                       ranked.score_version,
                       u.poptomo_id, up.user_name, up.display_popclass
                  FROM (
                        SELECT playdata_id, user_id, version_score AS score,
                               version_rank_code AS rank_code, medal_code,
                               current_version AS score_version
                          FROM playdata
                         WHERE chart_id = ? AND current_version = ?
                         ORDER BY version_score DESC, playdata_id
                         LIMIT ?
                       ) ranked
                  JOIN users u ON u.user_id = ranked.user_id
                  JOIN user_profiles up ON up.user_id = ranked.user_id
                 WHERE up.is_hidden = FALSE
                 ORDER BY ranked.score DESC, ranked.playdata_id
                """, chartId, currentVersion, limit);
        List<PlaydataQueryResults.RankingEntry> allTime = queryRankings("""
                SELECT ranked.score, ranked.rank_code, ranked.medal_code,
                       ranked.score_version,
                       u.poptomo_id, up.user_name, up.display_popclass
                  FROM (
                        SELECT playdata_id, user_id, all_time_score AS score,
                               all_time_rank_code AS rank_code, medal_code,
                               all_time_score_version AS score_version
                          FROM playdata
                         WHERE chart_id = ?
                         ORDER BY all_time_score DESC, playdata_id
                         LIMIT ?
                       ) ranked
                  JOIN users u ON u.user_id = ranked.user_id
                  JOIN user_profiles up ON up.user_id = ranked.user_id
                 WHERE up.is_hidden = FALSE
                 ORDER BY ranked.score DESC, ranked.playdata_id
                """, chartId, limit);
        return new PlaydataQueryResults.ChartRankings(chartId, current, allTime);
    }

    private List<PlaydataQueryResults.ChartPlaydata> queryPlaydata(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new PlaydataQueryResults.ChartPlaydata(
                rs.getLong("chart_id"), rs.getString("song_hash"),
                rs.getString("genre_name"), rs.getString("song_name"),
                rs.getInt("difficulty_code"), rs.getString("difficulty_label"),
                rs.getInt("level"), rs.getInt("chart_version"), rs.getBoolean("is_upper"),
                new PlaydataQueryResults.Best(
                        rs.getInt("version_score"), integer(rs, "version_rank_code"),
                        rs.getInt("current_version")),
                new PlaydataQueryResults.Best(
                        rs.getInt("all_time_score"), integer(rs, "all_time_rank_code"),
                        rs.getInt("all_time_score_version")),
                new PlaydataQueryResults.Medal(rs.getInt("medal_code")),
                rs.getInt("popclass"), rs.getString("popclass_bucket"),
                integer(rs, "popclass_bucket_rank")), args);
    }

    private List<PlaydataQueryResults.RankingEntry> queryRankings(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new PlaydataQueryResults.RankingEntry(
                rowNum + 1, rs.getString("poptomo_id"), rs.getString("user_name"),
                rs.getInt("display_popclass"), rs.getInt("score"),
                integer(rs, "rank_code"), rs.getInt("medal_code"),
                integer(rs, "score_version")), args);
    }

    private PlaydataQueryResults.ChartPlaydata withLegacyPopclass(
            PlaydataQueryResults.ChartPlaydata row) {
        int value = popclassPolicy.legacyChartPopclass(
                row.level(), row.allTimeBest().score(), row.medal().code());
        return new PlaydataQueryResults.ChartPlaydata(
                row.chartId(), row.songHash(), row.genreName(), row.songName(),
                row.difficultyCode(), row.difficultyLabel(), row.level(),
                row.chartVersion(), row.upper(), row.versionBest(), row.allTimeBest(),
                row.medal(), value, null, null);
    }

    private PlaydataQueryResults.ChartPlaydata withPotentialPopclass(
            PlaydataQueryResults.ChartPlaydata row) {
        int value = popclassPolicy.newChartPopclass(
                row.level(), row.allTimeBest().score(), row.medal().code());
        return copyPopclass(row, value, null, null);
    }

    private static List<PlaydataQueryResults.ChartPlaydata> rankedBucket(
            List<PlaydataQueryResults.ChartPlaydata> rows, String bucket) {
        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> copyPopclass(
                        rows.get(index), rows.get(index).popclass(), bucket, index + 1))
                .toList();
    }

    private static PlaydataQueryResults.ChartPlaydata copyPopclass(
            PlaydataQueryResults.ChartPlaydata row, int value,
            String bucket, Integer bucketRank) {
        return new PlaydataQueryResults.ChartPlaydata(
                row.chartId(), row.songHash(), row.genreName(), row.songName(),
                row.difficultyCode(), row.difficultyLabel(), row.level(),
                row.chartVersion(), row.upper(), row.versionBest(), row.allTimeBest(),
                row.medal(), value, bucket, bucketRank);
    }

    private UserSummary findUser(String poptomoId) {
        List<UserSummary> users = jdbc.query("""
                SELECT u.user_id, up.user_name, up.display_popclass,
                       up.potential_popclass, up.legacy_popclass
                  FROM users u JOIN user_profiles up ON up.user_id = u.user_id
                 WHERE u.poptomo_id = ?
                """, (rs, rowNum) -> new UserSummary(
                rs.getLong("user_id"), rs.getString("user_name"),
                rs.getInt("display_popclass"), rs.getInt("potential_popclass"),
                rs.getInt("legacy_popclass")), poptomoId);
        if (users.size() != 1) throw new IllegalArgumentException("User was not found.");
        return users.getFirst();
    }

    private void requireChart(long chartId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM charts WHERE chart_id = ? AND is_deleted = FALSE",
                Integer.class, chartId);
        if (count == null || count != 1) {
            throw new IllegalArgumentException("Chart was not found.");
        }
    }

    private static Integer integer(java.sql.ResultSet rs, String column)
            throws java.sql.SQLException {
        return (Integer) rs.getObject(column);
    }

    private record UserSummary(long userId, String userName, int displayPopclass,
                               int potentialPopclass, int legacyPopclass) {
    }
}
