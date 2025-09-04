package gg.popn.domain.playdata.application.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum TargetOption {
    MEDAL,
    RANK;

    @JsonCreator
    public static TargetOption from(String s) {
        if (s == null) {
            return null;
        }
        return TargetOption.valueOf(s.toUpperCase());
    }
}
