package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.dto.result.SongChartView;
import gg.popn.application.song.dto.result.ChartDetailView;
import gg.popn.application.song.dto.result.ChartMetadataView;
import gg.popn.application.song.dto.result.DifficultyView;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.dto.result.SongMetadataView;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import gg.popn.domain.game.policy.DifficultyPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SongCatalogJdbcAdapter implements SongCatalogQueryPort {
    private static final String SONG_FILTERS = """
            FROM songs s
            WHERE (:version IS NULL OR s.version = :version)
              AND (
                :keyword IS NULL
                OR LOWER(s.song_name) LIKE :keywordPattern
                OR LOWER(s.genre_name) LIKE :keywordPattern
                OR LOWER(COALESCE(s.artist_name, '')) LIKE :keywordPattern
                OR EXISTS (
                    SELECT 1
                    FROM song_search_tags st
                    WHERE st.song_id = s.song_id
                      AND st.is_active = TRUE
                      AND LOWER(st.normalized_tag_value) LIKE :keywordPattern
                )
              )
              AND EXISTS (
                SELECT 1
                FROM charts cf
                WHERE cf.song_id = s.song_id
                  AND cf.is_deleted = FALSE
                  AND (:chartVersion IS NULL OR cf.chart_version = :chartVersion)
                  AND (:levelMin IS NULL OR cf.level >= :levelMin)
                  AND (:levelMax IS NULL OR cf.level <= :levelMax)
                  AND (:hasDifficulties = FALSE OR cf.difficulty_code IN (:difficulties))
                  AND (:isUpper IS NULL OR cf.is_upper = :isUpper)
                  AND (:hasStrictGauge IS NULL OR cf.has_strict_gauge = :hasStrictGauge)
                  AND (:hasStrictJudgement IS NULL OR cf.has_strict_judgement = :hasStrictJudgement)
              )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public long count(FindSongsQuery query) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) " + SONG_FILTERS, parameters(query), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<GroupedSongView> findPage(FindSongsQuery query) {
        MapSqlParameterSource parameters = parameters(query)
                .addValue("limit", query.size())
                .addValue("offset", query.page() * query.size());

        List<SongRow> songs = jdbcTemplate.query("""
                        SELECT s.song_id, s.song_hash, s.genre_name, s.song_name,
                               s.artist_name, s.version, s.jacket_url,
                               (SELECT MAX(cs.level) FROM charts cs
                                 WHERE cs.song_id = s.song_id AND cs.is_deleted = FALSE) AS max_level
                        """ + SONG_FILTERS + " ORDER BY " + orderBy(query) + """
                        , s.song_id ASC
                        LIMIT :limit OFFSET :offset
                        """,
                parameters,
                (rs, rowNum) -> new SongRow(
                        rs.getLong("song_id"),
                        rs.getString("song_hash"),
                        rs.getString("genre_name"),
                        rs.getString("song_name"),
                        rs.getString("artist_name"),
                        rs.getInt("version"),
                        rs.getString("jacket_url")));

        if (songs.isEmpty()) {
            return List.of();
        }

        List<Long> songIds = songs.stream().map(SongRow::songId).toList();
        parameters.addValue("songIds", songIds);
        Map<Long, List<SongChartView>> chartsBySong = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT c.chart_id, c.song_id, c.level, c.difficulty_code,
                               CASE WHEN c.difficulty_code = 1 THEN 'LIGHT' ELSE c.difficulty_label END AS difficulty_label,
                               c.chart_version, c.is_upper, c.has_strict_gauge, c.has_strict_judgement
                        FROM charts c
                        WHERE c.song_id IN (:songIds)
                          AND c.is_deleted = FALSE
                          AND (:includeAllCharts = TRUE OR :chartVersion IS NULL OR c.chart_version = :chartVersion)
                          AND (:includeAllCharts = TRUE OR :levelMin IS NULL OR c.level >= :levelMin)
                          AND (:includeAllCharts = TRUE OR :levelMax IS NULL OR c.level <= :levelMax)
                          AND (:includeAllCharts = TRUE OR :hasDifficulties = FALSE OR c.difficulty_code IN (:difficulties))
                          AND (:includeAllCharts = TRUE OR :isUpper IS NULL OR c.is_upper = :isUpper)
                          AND (:includeAllCharts = TRUE OR :hasStrictGauge IS NULL OR c.has_strict_gauge = :hasStrictGauge)
                          AND (:includeAllCharts = TRUE OR :hasStrictJudgement IS NULL OR c.has_strict_judgement = :hasStrictJudgement)
                        ORDER BY c.song_id, c.difficulty_code, c.is_upper
                        """,
                parameters,
                rs -> {
                    long songId = rs.getLong("song_id");
                    chartsBySong.computeIfAbsent(songId, ignored -> new ArrayList<>())
                            .add(new SongChartView(
                                    rs.getLong("chart_id"),
                                    rs.getInt("level"),
                                    rs.getInt("difficulty_code"),
                                    rs.getString("difficulty_label"),
                                    rs.getInt("chart_version"),
                                    rs.getBoolean("is_upper"),
                                    rs.getBoolean("has_strict_gauge"),
                                    rs.getBoolean("has_strict_judgement")));
                });

        return songs.stream()
                .map(song -> new GroupedSongView(
                        song.songId(), song.songHash(), song.genreName(), song.songName(),
                        song.artistName(), song.version(), song.jacketUrl(),
                        List.copyOf(chartsBySong.getOrDefault(song.songId(), List.of()))))
                .toList();
    }

    @Override
    public Optional<SongDetailView> findSongDetail(long songId) {
        return findSongDetail("song_id", songId);
    }

    @Override
    public Optional<SongDetailView> findSongDetail(String songHash) {
        return findSongDetail("song_hash", songHash);
    }

    private Optional<SongDetailView> findSongDetail(String column, Object value) {
        String songSql = """
                        SELECT song_id, song_hash, genre_name, song_name, artist_name, version, jacket_url
                        FROM songs
                        WHERE %s = :value
                        """.formatted(column);
        List<SongMetadataView> songs = jdbcTemplate.query(songSql,
                new MapSqlParameterSource("value", value),
                (rs, rowNum) -> songMetadata(rs));
        if (songs.isEmpty()) {
            return Optional.empty();
        }
        List<ChartMetadataView> charts = jdbcTemplate.query("""
                        SELECT chart_id, difficulty_code, level, chart_version, is_upper,
                               has_strict_gauge, has_strict_judgement, is_deleted
                        FROM charts
                        WHERE song_id = :songId
                        ORDER BY difficulty_code, is_upper
                        """,
                new MapSqlParameterSource("songId", songs.getFirst().songId()),
                (rs, rowNum) -> chartMetadata(rs));
        return Optional.of(new SongDetailView(songs.getFirst(), charts));
    }

    @Override
    public Optional<ChartDetailView> findChartDetail(long chartId) {
        List<ChartDetailView> details = jdbcTemplate.query("""
                        SELECT s.song_id, s.song_hash, s.genre_name, s.song_name, s.artist_name,
                               s.version, s.jacket_url, c.chart_id, c.difficulty_code, c.level,
                               c.chart_version, c.is_upper, c.has_strict_gauge,
                               c.has_strict_judgement, c.is_deleted
                        FROM charts c
                        JOIN songs s ON s.song_id = c.song_id
                        WHERE c.chart_id = :chartId
                        """,
                new MapSqlParameterSource("chartId", chartId),
                (rs, rowNum) -> new ChartDetailView(songMetadata(rs), chartMetadata(rs)));
        return details.stream().findFirst();
    }

    private SongMetadataView songMetadata(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SongMetadataView(
                rs.getLong("song_id"),
                rs.getString("song_hash"),
                rs.getString("genre_name"),
                rs.getString("song_name"),
                rs.getString("artist_name"),
                rs.getInt("version"),
                rs.getString("jacket_url"));
    }

    private ChartMetadataView chartMetadata(java.sql.ResultSet rs) throws java.sql.SQLException {
        DifficultyPolicy difficulty = DifficultyPolicy.fromCode(rs.getInt("difficulty_code"));
        return new ChartMetadataView(
                rs.getLong("chart_id"),
                new DifficultyView(difficulty.getCode(), difficulty.getLabel(),
                        difficulty.getShortLabel(), difficulty.getSortOrder()),
                rs.getInt("level"),
                rs.getInt("chart_version"),
                rs.getBoolean("is_upper"),
                rs.getBoolean("has_strict_gauge"),
                rs.getBoolean("has_strict_judgement"),
                rs.getBoolean("is_deleted"));
    }

    private MapSqlParameterSource parameters(FindSongsQuery query) {
        String normalizedKeyword = query.keyword() == null ? null : query.keyword().toLowerCase();
        return new MapSqlParameterSource()
                .addValue("keyword", normalizedKeyword)
                .addValue("keywordPattern", normalizedKeyword == null ? null : "%" + normalizedKeyword + "%")
                .addValue("version", query.version())
                .addValue("chartVersion", query.chartVersion())
                .addValue("levelMin", query.levelMin())
                .addValue("levelMax", query.levelMax())
                .addValue("hasDifficulties", query.difficulties() != null)
                .addValue("difficulties", query.difficulties() == null
                        ? List.of(-1) : query.difficulties())
                .addValue("isUpper", query.isUpper())
                .addValue("hasStrictGauge", query.hasStrictGauge())
                .addValue("hasStrictJudgement", query.hasStrictJudgement())
                .addValue("includeAllCharts", query.includeAllCharts());
    }

    private String orderBy(FindSongsQuery query) {
        String column = switch (query.sort()) {
            case VERSION -> "s.version";
            case TITLE -> "s.song_name";
            case GENRE -> "s.genre_name";
            case MAX_LEVEL -> "max_level";
            case SONG_ID -> "s.song_id";
        };
        String direction = query.order() == FindSongsQuery.Order.ASC ? "ASC" : "DESC";
        return column + " " + direction;
    }

    private record SongRow(
            long songId,
            String songHash,
            String genreName,
            String songName,
            String artistName,
            int version,
            String jacketUrl
    ) {
    }
}
