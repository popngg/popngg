package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;
import java.util.regex.Pattern;

@Builder
@Value
public class PoptomoId implements Validatable {

    private static final Pattern VALID_POPTOMO_ID_PATTERN =
            Pattern.compile("^\\d{4}-\\d{4}-\\d{4}$");

    String poptomoId;

    @JsonCreator
    public static PoptomoId of(String poptomoId) {
        if (poptomoId == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> PoptomoId.builder()
                .poptomoId(poptomoId)
                .build());
    }

    @JsonValue
    public String getValue() {
        return poptomoId;
    }

    @Override
    public void validate() {
        if (poptomoId == null) {
            throw new InvalidArgumentException("poptomoId", "It should not be empty.");
        }
        if (!VALID_POPTOMO_ID_PATTERN.matcher(poptomoId).matches()) {
            throw new InvalidArgumentException("poptomoId", "It should be in the format of XXXX-XXXX-XXXX.");
        }
    }
}
