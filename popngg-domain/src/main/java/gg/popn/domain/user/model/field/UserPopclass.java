package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UserPopclass implements Validatable {

    Integer popclass;

    public static UserPopclass of(Integer popclass) {
        if (popclass == null) {
            return null;
        }

        return UserPopclass.builder()
                .popclass(popclass)
                .build();
    }

    @JsonValue
    public Integer getValue() {
        return popclass;
    }

    @Override
    public void validate() {
        if (popclass == null) {
            throw new InvalidArgumentException("popclass", "It should not be empty.");
        }

        if (popclass < 0) {
            throw new InvalidArgumentException("popclass", "It should be greater than 0.");
        }
    }
}
