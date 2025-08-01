package gg.popn.domain.common.model;

import gg.popn.domain.chart.model.*;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Chart {
    SongHash songHash;
    GenreName genreName;
    SongName songName;
    Version version;
    Difficulty difficulty;
    Level level;
    IsUpper isUpper;
}
