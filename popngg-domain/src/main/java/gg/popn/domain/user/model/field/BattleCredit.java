package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class BattleCredit implements Validatable {

    Integer battleCredit;

    @JsonCreator
    public static BattleCredit of(Integer battleCredit) {
        if (battleCredit == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> BattleCredit.builder()
                .battleCredit(battleCredit)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return battleCredit;
    }

    @Override
    public void validate() {
        if (battleCredit == null) {
            throw new InvalidArgumentException("battleCredit", "It should not be empty.");
        }
    }
}
