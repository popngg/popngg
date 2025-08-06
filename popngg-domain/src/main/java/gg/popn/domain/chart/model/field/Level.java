package gg.popn.domain.chart.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Level implements Validatable {

    Integer level;

    public static Level of(Integer level) {
        if (level == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Level.builder()
                .level(level)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return level;
    }

    @Override
    public void validate() {
        if (level == null) {
            throw new InvalidArgumentException("level", "It should not be empty.");
        }

        if (level <= 0 || level >= 51) {
            throw new InvalidArgumentException("level", "It should be between 1 and 50.");
        }
    }
}
