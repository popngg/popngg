package gg.popn.domain.chart.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class SongName {

    String songName;

    public static SongName of(String songName) {
        if (songName == null) {
            return null;
        }

        return SongName.builder()
                .songName(songName)
                .build();
    }

    @JsonValue
    public String getValue() {
        return songName;
    }
}
