package gg.popn.domain.playdata.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Score implements Validatable {

    Integer score;

    @JsonCreator
    public static Score of(Integer score) {
        if (score == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Score.builder()
                .score(score)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return score;
    }

    @Override
    public void validate() {
        if (score == null) {
            throw new InvalidArgumentException("score", "It should not be empty.");
        }
    }
}
