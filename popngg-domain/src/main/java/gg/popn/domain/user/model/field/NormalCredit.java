package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class NormalCredit implements Validatable {

    Integer normalCredit;

    @JsonCreator
    public static NormalCredit of(Integer normalCredit) {
        if (normalCredit == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> NormalCredit.builder()
                .normalCredit(normalCredit)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return normalCredit;
    }

    @Override
    public void validate() {
        if (normalCredit == null) {
            throw new InvalidArgumentException("normalCredit", "It should not be empty.");
        }
    }
}
