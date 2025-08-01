package gg.popn.domain.chart.model;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Version implements Validatable {

    Integer version;
    String versionName;

    public static Version of(Integer version) {
        if (version == null) {
            return null;
        }

        return Version.builder()
                .version(version)
                .versionName(getVersionName(version))
                .build();
    }

    @JsonValue
    public Integer getValue() {
        return version;
    }

    @Override
    public void validate() {
        if (version == null) {
            throw new InvalidArgumentException("version", "It should not be empty.");
        }

        if ((version < 1 || version > 28) && version != 99) {
            throw new InvalidArgumentException("version", "Version should be between 1 and 28 or 99.");
        }
    }

    public static String getVersionName(Integer version) {
        return switch (version) {
            case 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 -> String.valueOf(version);
            case 12 -> "いろは";
            case 13 -> "カーニバル";
            case 14 -> "FEVER!";
            case 15 -> "ADVENTURE";
            case 16 -> "PARTY♪";
            case 17 -> "THE MOVIE";
            case 18 -> "せんごく列伝";
            case 19 -> "TUNE STREET";
            case 20 -> "fantasia";
            case 21 -> "Sunny Park";
            case 22 -> "ラピストリア";
            case 23 -> "éclale";
            case 24 -> "うさぎと猫と少年の夢";
            case 25 -> "peace";
            case 26 -> "解明リドルズ";
            case 27 -> "UniLab";
            case 28 -> "Jam&Fizz";
            case 99 -> "ETC";
            default -> throw new InvalidArgumentException("version", "Version should be between 1 and 28 or 99.");
        };
    }
}
