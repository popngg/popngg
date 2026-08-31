package gg.popn.infra.db.adapter;

import gg.popn.application.song.dto.command.UpdateSongCommand;
import gg.popn.application.song.port.out.UpdateSongPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import gg.popn.domain.game.policy.DifficultyPolicy;

@Repository @RequiredArgsConstructor
public class UpdateSongJdbcAdapter implements UpdateSongPort {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void update(UpdateSongCommand command, String songHash) {
        var songParams = new MapSqlParameterSource()
                .addValue("songId", command.songId()).addValue("songHash", songHash)
                .addValue("genreName", command.genreName()).addValue("songName", command.songName())
                .addValue("artistName", command.artistName()).addValue("version", command.version())
                .addValue("jacketUrl", command.jacketUrl())
                .addValue("createdAt", command.createdAt() == null ? null : Timestamp.from(command.createdAt()));
        int songs = jdbc.update("""
                UPDATE songs SET song_hash=:songHash, genre_name=:genreName, song_name=:songName,
                    artist_name=:artistName, version=:version, jacket_url=:jacketUrl,
                    created_at=COALESCE(:createdAt,created_at), updated_at=CURRENT_TIMESTAMP
                WHERE song_id=:songId
                """, songParams);
        if (songs != 1) throw new IllegalArgumentException("Song was not found.");

        for (var chart : command.charts()) {
            if (chart.chartId() == null) {
                DifficultyPolicy difficulty = DifficultyPolicy.fromCode(chart.difficultyCode());
                jdbc.update("""
                        INSERT INTO charts (song_id,difficulty_code,difficulty_label,level,chart_version,
                            is_upper,has_strict_gauge,has_strict_judgement,is_deleted,created_at,updated_at)
                        VALUES (:songId,:difficulty,:label,:level,:chartVersion,:upper,
                            :strictGauge,:strictJudgement,FALSE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """, new MapSqlParameterSource().addValue("songId", command.songId())
                        .addValue("difficulty", difficulty.getCode()).addValue("label", difficulty.getLabel())
                        .addValue("level", chart.level()).addValue("chartVersion", chart.chartVersion())
                        .addValue("upper", chart.isUpper()).addValue("strictGauge", chart.hasStrictGauge())
                        .addValue("strictJudgement", chart.hasStrictJudgement()));
                continue;
            }
            int updated = jdbc.update("""
                    UPDATE charts SET level=:level, chart_version=:chartVersion,
                        is_upper=:upper, has_strict_gauge=:strictGauge,
                        has_strict_judgement=:strictJudgement, updated_at=CURRENT_TIMESTAMP
                    WHERE chart_id=:chartId AND song_id=:songId AND is_deleted=FALSE
                    """, new MapSqlParameterSource().addValue("songId", command.songId())
                    .addValue("chartId", chart.chartId()).addValue("level", chart.level())
                    .addValue("chartVersion", chart.chartVersion()).addValue("upper", chart.isUpper())
                    .addValue("strictGauge", chart.hasStrictGauge())
                    .addValue("strictJudgement", chart.hasStrictJudgement()));
            if (updated != 1) throw new IllegalArgumentException("Chart does not belong to the song: " + chart.chartId());
        }
    }
}
