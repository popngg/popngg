package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Character implements Validatable {

    String character;

    @JsonCreator
    public static Character of(String character) {
        if (character == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Character.builder()
                .character(character)
                .build());
    }

    @JsonValue
    public String getValue() {
        return character;
    }

    @Override
    public void validate() {
        if (character == null || character.isBlank()) {
            throw new InvalidArgumentException("character", "It should not be empty.");
        }
    }
}
