package gg.popn.domain.playdata.application.dto;

import gg.popn.domain.chart.model.field.Difficulty;
import gg.popn.domain.chart.model.field.GenreName;
import gg.popn.domain.chart.model.field.IsUpper;
import gg.popn.domain.chart.model.field.SongName;
import gg.popn.domain.chart.model.field.Version;
import gg.popn.domain.playdata.model.field.Medal;
import gg.popn.domain.playdata.model.field.Rank;
import gg.popn.domain.playdata.model.field.Score;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaydataInputDto {//갱신코드 사용 유지를 위한 이름
    GenreName genreName;
    SongName songName;
    Version version;
    Difficulty difficulty;
    Medal medal;
    Rank rank;
    Score score;
    IsUpper isUpper;
}