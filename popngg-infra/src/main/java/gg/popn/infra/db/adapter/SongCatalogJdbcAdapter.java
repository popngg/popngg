package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.dto.result.GroupedSongView;
import gg.popn.application.song.dto.result.SongChartView;
import gg.popn.application.song.port.out.SongCatalogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                  AND (:level IS NULL OR cf.level = :level)
                  AND (:difficulty IS NULL OR cf.difficulty_code = :difficulty)
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
                               s.artist_name, s.version, s.jacket_url
                        """ + SONG_FILTERS + """
                        ORDER BY s.song_id
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
                          AND (:chartVersion IS NULL OR c.chart_version = :chartVersion)
                          AND (:level IS NULL OR c.level = :level)
                          AND (:difficulty IS NULL OR c.difficulty_code = :difficulty)
                          AND (:isUpper IS NULL OR c.is_upper = :isUpper)
                          AND (:hasStrictGauge IS NULL OR c.has_strict_gauge = :hasStrictGauge)
                          AND (:hasStrictJudgement IS NULL OR c.has_strict_judgement = :hasStrictJudgement)
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

    private MapSqlParameterSource parameters(FindSongsQuery query) {
        String normalizedKeyword = query.keyword() == null ? null : query.keyword().toLowerCase();
        return new MapSqlParameterSource()
                .addValue("keyword", normalizedKeyword)
                .addValue("keywordPattern", normalizedKeyword == null ? null : "%" + normalizedKeyword + "%")
                .addValue("version", query.version())
                .addValue("chartVersion", query.chartVersion())
                .addValue("level", query.level())
                .addValue("difficulty", query.difficulty())
                .addValue("isUpper", query.isUpper())
                .addValue("hasStrictGauge", query.hasStrictGauge())
                .addValue("hasStrictJudgement", query.hasStrictJudgement());
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
