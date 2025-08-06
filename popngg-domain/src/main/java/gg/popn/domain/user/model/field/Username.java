package gg.popn.domain.user.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

import java.util.regex.Pattern;

@Builder
@Value
public class Username implements Validatable {

    private static final Pattern VALID_USERNAME_PATTERN =
            Pattern.compile("^[\\p{InHiragana}\\p{InKatakana}\\p{InCjkUnifiedIdeographs}&&[^\\p{InBasicLatin}]]+$");

    String username;

    public static Username of(String username) {
        if (username == null) {
            return null;
        }

        return Username.builder()
                .username(username)
                .build();
    }

    @JsonValue
    public String getValue() {
        return username;
    }

    @Override
    public void validate() {
        if (username == null) {
            throw new InvalidArgumentException("username", "It should not be empty.");
        }
        if (username.length() > 6) {
            throw new InvalidArgumentException("username", "It should not exceed 6 characters.");
        }
        if (!VALID_USERNAME_PATTERN.matcher(username).matches()) {
            throw new InvalidArgumentException("username", "It should only contain Hiragana, Katakana, and full-width characters.");
        }
    }
}
