package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Comment implements Validatable {

    private static final int MAX_LENGTH = 255;

    String comment;

    @JsonCreator
    public static Comment of(String comment) {
        if (comment == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Comment.builder()
                .comment(comment)
                .build());
    }

    @JsonValue
    public String getValue() {
        return comment;
    }

    @Override
    public void validate() {
        if (comment != null && comment.length() > MAX_LENGTH) {
            throw new InvalidArgumentException("comment", "It should be " + MAX_LENGTH + " characters or less.");
        }
    }
}
