package gg.popn.domain.playdata.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Popclass implements Validatable {

    Integer popclass;

    @JsonCreator
    public static Popclass of(Integer popclass) {
        if (popclass == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Popclass.builder()
                .popclass(popclass)
                .build());
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
    }
}
