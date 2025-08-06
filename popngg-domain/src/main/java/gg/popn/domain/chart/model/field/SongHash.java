package gg.popn.domain.chart.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Builder
@Value
public class SongHash {

    String songHash;

    public static SongHash of(String songHash) {
        if (songHash == null) {
            return null;
        }

        return SongHash.builder()
                .songHash(songHash)
                .build();
    }

    @JsonValue
    public String getValue() {
        return songHash;
    }

    public static SongHash from(GenreName genreName, SongName songName, Version version, IsUpper isUpper) {
        StringBuilder songHashBuilder = new StringBuilder()
                .append(genreName.getValue())
                .append(songName.getValue())
                .append(version.getValue().toString());

        if (exemption.contains(genreName.getValue()) && isUpper.getValue().equals(1)) {
            songHashBuilder.append("2");
        }

        return SongHash.of(
                DigestUtils.md5DigestAsHex(songHashBuilder.toString().getBytes(StandardCharsets.UTF_8)).toLowerCase()
        );
    }

    private static final List<String> exemption = List.of(
            "virkatoの主題によるperson09風超絶技巧変奏曲",
            "Popperz Chronicle",
            "ma plume",
            "Megalara Garuda"
    );
}
