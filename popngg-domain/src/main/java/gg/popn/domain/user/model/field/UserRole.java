

package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class UserRole implements Validatable {

    String role;

    public static UserRole of(String role) {
        if (role == null) {
            return null;
        }

        return UserRole.builder()
                .role(role)
                .build();
    }

    @JsonValue
    public String getValue() {
        return role;
    }

    @Override
    public void validate() {
        if (role == null) {
            throw new InvalidArgumentException("username", "It should not be empty.");
        }

        if (!validRoles.contains(role)) {
            throw new InvalidArgumentException("role", "It should be USER or ADMIN.");
        }
    }

    private static final List<String> validRoles =
        List.of(
                "USER",
                "ADMIN"
        );
}
