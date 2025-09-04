package gg.popn.domain.playdata.application.dto;

import gg.popn.domain.chart.model.field.*;
import gg.popn.domain.playdata.model.Playdata;
import gg.popn.domain.playdata.model.field.Medal;
import gg.popn.domain.playdata.model.field.Popclass;
import gg.popn.domain.playdata.model.field.Rank;
import gg.popn.domain.playdata.model.field.Score;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.Username;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class PlaydataDto {
    GenreName genreName;
    SongName songName;
    IsUpper isUpper;
    Version version;
    Difficulty difficulty;
    Level level;
    Score score;
    Medal medal;
    Rank rank;
    Popclass popclass;
    SongHash songHash;

    public static PlaydataDto from(Playdata playdata) {
        return PlaydataDto.builder()
                .songHash(playdata.getChart().getSongHash())
                .songName(playdata.getChart().getSongName())
                .genreName(playdata.getChart().getGenreName())
                .isUpper(playdata.getChart().getIsUpper())
                .version(playdata.getChart().getVersion())
                .difficulty(playdata.getChart().getDifficulty())
                .level(playdata.getChart().getLevel())
                .score(playdata.getScore())
                .medal(playdata.getMedal())
                .rank(playdata.getRank())
                .popclass(playdata.getPopclass())
                .build();
    }
}
