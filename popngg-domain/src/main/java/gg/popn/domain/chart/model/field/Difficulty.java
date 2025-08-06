package gg.popn.domain.chart.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Difficulty implements Validatable {

    Integer difficulty;

    public static Difficulty of(Integer difficulty) {
        if (difficulty == null) {
            return null;
        }

        return Difficulty.builder()
                .difficulty(difficulty)
                .build();
    }

    @JsonValue
    public Integer getValue() {
        return difficulty;
    }

    @Override
    public void validate() {
        if (difficulty == null) {
            throw new InvalidArgumentException("difficulty", "It should not be empty.");
        }

        if (difficulty <= 0 || difficulty >= 5) {
            throw new InvalidArgumentException("difficulty", "It should be between 1 and 4.");
        }
    }
}
