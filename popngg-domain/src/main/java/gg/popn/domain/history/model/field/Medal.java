package gg.popn.domain.history.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Medal implements Validatable {

    Integer medal;

    @JsonCreator
    public static Medal of(Integer medal) {
        if (medal == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Medal.builder()
                .medal(medal)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return medal;
    }

    @Override
    public void validate() {
        if (medal == null) {
            throw new InvalidArgumentException("medal", "It should not be empty.");
        }
    }
}