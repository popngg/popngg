package gg.popn.domain.common.model;

import gg.popn.domain.chart.model.Difficulty;
import gg.popn.domain.chart.model.IsUpper;
import gg.popn.domain.chart.model.Level;
import gg.popn.domain.chart.model.Version;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Chart {
    String songHash;
    String genreName;
    String songName;
    Version version;
    Difficulty difficulty;
    Level level;
    IsUpper isUpper;

    @Override
    public String toString() {
        return "Chart{" +
                "songHash='" + songHash + '\'' +
                ", genreName='" + genreName + '\'' +
                ", songName='" + songName + '\'' +
                ", version=" + version +
                ", difficulty=" + difficulty +
                ", level=" + level +
                ", isUpper=" + isUpper +
                '}';
    }
}
