package gg.popn.domain.common.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Chart {
    String songHash;
    String genreName;
    String songName;
    Integer version;
    Integer difficulty;
    Integer level;
    Integer isUpper;

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
