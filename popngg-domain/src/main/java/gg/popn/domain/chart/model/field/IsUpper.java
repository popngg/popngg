package gg.popn.domain.chart.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class IsUpper implements Validatable {

    Integer isUpper;

    public static IsUpper of(Integer isUpper) {
        if (isUpper == null) {
            return null;
        }

        return IsUpper.builder()
                .isUpper(isUpper)
                .build();
    }

    @JsonValue
    public Integer getValue() {
        return isUpper;
    }

    @Override
    public void validate() {
        if (isUpper == null) {
            throw new InvalidArgumentException("isUpper", "It should not be empty.");
        }

        if (isUpper != 0 && isUpper != 1) {
            throw new InvalidArgumentException("isUpper", "It should be 0 or 1.");
        }
    }
}
