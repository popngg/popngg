package gg.popn.application.playdata.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum GroupByOption {
    DIFFICULTY,
    LEVEL;

    @JsonCreator
    public static GroupByOption from(String s) {
        if (s == null) {
            return null;
        }
        return GroupByOption.valueOf(s.toUpperCase());
    }
}
