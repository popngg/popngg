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
public class Password implements Validatable {

    private static final int PASSWORD_LENGTH = 64;
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^[a-f0-9]{64}$");

    String password;

    @JsonCreator
    public static Password of(String password) {
        if (password == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Password.builder()
                .password(password)
                .build());
    }

    @JsonValue
    public String getValue() {
        return password;
    }

    @Override
    public void validate() {
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidArgumentException("password", "It should not be empty.");
        }

        if (password.length() != 64) {
            throw new InvalidArgumentException("password", "Password length should be 64.");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidArgumentException("password", "Password should be consisted with a-f and 0-9.");
        }
    }
}
