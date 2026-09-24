package gg.popn.infra.db.adapter;

import gg.popn.application.chart.port.out.ChartQueryPort;
import gg.popn.domain.chart.model.Chart;
import gg.popn.domain.chart.model.field.*;
import gg.popn.domain.common.exception.ChartNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Legacy v2 responses backed by the current normalized catalog. */
@Repository
@RequiredArgsConstructor
public class ChartQueryJdbcAdapter implements ChartQueryPort {
    private static final String SELECT = """
            SELECT s.song_hash, s.genre_name, s.song_name, s.version,
                   c.difficulty_code, c.level, c.is_upper
            FROM charts c JOIN songs s ON s.song_id = c.song_id
            WHERE c.is_deleted = FALSE
            """;
    private static final RowMapper<Chart> MAPPER = (rs, rowNum) -> Chart.builder()
            .songHash(SongHash.of(rs.getString("song_hash")))
            .genreName(GenreName.of(rs.getString("genre_name")))
            .songName(SongName.of(rs.getString("song_name")))
            .version(Version.of(rs.getInt("version")))
            .difficulty(Difficulty.of(rs.getInt("difficulty_code")))
            .level(Level.of(rs.getInt("level")))
            .isUpper(IsUpper.of(rs.getInt("is_upper")))
            .build();
    private final JdbcTemplate jdbc;

    @Override
    public Chart findBySongHashAndDifficulty(SongHash hash, Difficulty difficulty) {
        return findListBySongHashAndDifficulty(hash, difficulty).stream().findFirst()
                .orElseThrow(ChartNotFoundException::new);
    }

    @Override
    public List<Chart> findAllCharts() {
        return jdbc.query(SELECT + " ORDER BY c.chart_id", MAPPER);
    }

    @Override
    public List<Chart> findListBySongHash(SongHash hash) {
        return jdbc.query(SELECT + " AND s.song_hash = ? ORDER BY c.chart_id", MAPPER, hash.getValue());
    }

    @Override
    public List<Chart> findListBySongHashAndDifficulty(SongHash hash, Difficulty difficulty) {
        return jdbc.query(SELECT + " AND s.song_hash = ? AND c.difficulty_code = ? ORDER BY c.is_upper, c.chart_id",
                MAPPER, hash.getValue(), difficulty.getValue());
    }

    @Override
    public List<Chart> findRecentCharts(int limit) {
        return jdbc.query(SELECT + " ORDER BY c.created_at DESC, c.chart_id DESC LIMIT ?", MAPPER, limit);
    }
}
