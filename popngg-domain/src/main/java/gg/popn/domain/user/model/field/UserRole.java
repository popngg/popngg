package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Builder
@Value
public class UserRole implements Validatable {

    private static final List<String> validRoles = Arrays.asList("USER", "ADMIN");

    String role;

    @JsonCreator
    public static UserRole of(String role) {
        if (role == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> UserRole.builder()
                .role(role)
                .build());
    }

    @JsonValue
    public String getValue() {
        return role;
    }

    @Override
    public void validate() {
        if (role == null) {
            throw new InvalidArgumentException("role", "It should not be empty.");
        }

        if (!validRoles.contains(role)) {
            throw new InvalidArgumentException("role", "It should be USER or ADMIN.");
        }
    }
}
