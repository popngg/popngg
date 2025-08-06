package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class IsHidden implements Validatable {

    Integer isHidden;

    public static IsHidden of(Integer isHidden) {
        if (isHidden == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> IsHidden.builder()
                .isHidden(isHidden)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return isHidden;
    }

    @Override
    public void validate() {
        if (isHidden == null) {
            throw new InvalidArgumentException("isHidden", "It should not be empty.");
        }

        if (isHidden != 0 && isHidden != 1) {
            throw new InvalidArgumentException("isHidden", "It should be 0 or 1.");
        }
    }
}
