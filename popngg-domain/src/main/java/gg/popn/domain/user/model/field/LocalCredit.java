package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class LocalCredit implements Validatable {

    Integer localCredit;

    @JsonCreator
    public static LocalCredit of(Integer localCredit) {
        if (localCredit == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> LocalCredit.builder()
                .localCredit(localCredit)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return localCredit;
    }

    @Override
    public void validate() {
        if (localCredit == null) {
            throw new InvalidArgumentException("localCredit", "It should not be empty.");
        }
    }
}
