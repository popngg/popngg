package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.out.CreateSongPort;
import gg.popn.domain.game.policy.DifficultyPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class CreateSongJdbcAdapter implements CreateSongPort {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public CreateSongResult create(CreateSongCommand command) {
        long songId = insertSong(command);
        List<Long> chartIds = new ArrayList<>();
        for (CreateSongCommand.CreateChartCommand chart : command.charts()) {
            chartIds.add(insertChart(songId, chart));
        }
        return new CreateSongResult(songId, List.copyOf(chartIds));
    }

    private long insertSong(CreateSongCommand command) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                        INSERT INTO songs (
                            song_hash, genre_name, song_name, artist_name, version, jacket_url,
                            created_at, updated_at
                        ) VALUES (
                            :songHash, :genreName, :songName, :artistName, :version, :jacketUrl,
                            :createdAt, CURRENT_TIMESTAMP
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("songHash", command.songHash())
                        .addValue("genreName", command.genreName())
                        .addValue("songName", command.songName())
                        .addValue("artistName", command.artistName())
                        .addValue("version", command.version())
                        .addValue("jacketUrl", command.jacketUrl())
                        .addValue("createdAt", Timestamp.from(command.createdAt() == null
                                ? Instant.now() : command.createdAt())),
                keyHolder,
                new String[]{"song_id"});
        return keyHolder.getKey().longValue();
    }

    private long insertChart(long songId, CreateSongCommand.CreateChartCommand chart) {
        DifficultyPolicy difficulty = DifficultyPolicy.fromCode(chart.difficulty());
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                        INSERT INTO charts (
                            song_id, difficulty_code, difficulty_label, level, chart_version,
                            has_strict_judgement, has_strict_gauge, is_upper, is_deleted,
                            created_at, updated_at
                        ) VALUES (
                            :songId, :difficulty, :label, :level, :chartVersion,
                            :strictJudgement, :strictGauge, :upper, FALSE,
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("songId", songId)
                        .addValue("difficulty", difficulty.getCode())
                        .addValue("label", difficulty.getLabel())
                        .addValue("level", chart.level())
                        .addValue("chartVersion", chart.chartVersion())
                        .addValue("strictJudgement", chart.hasStrictJudgement())
                        .addValue("strictGauge", chart.hasStrictGauge())
                        .addValue("upper", chart.isUpper()),
                keyHolder,
                new String[]{"chart_id"});
        return keyHolder.getKey().longValue();
    }
}
