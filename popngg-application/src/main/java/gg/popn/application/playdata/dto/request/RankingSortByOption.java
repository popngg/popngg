package gg.popn.application.playdata.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RankingSortByOption {
    SCORE,
    MEDAL;

    @JsonCreator
    public static RankingSortByOption from(String s) {
        if (s == null) return null;
        return RankingSortByOption.valueOf(s.toUpperCase());
    }
}
