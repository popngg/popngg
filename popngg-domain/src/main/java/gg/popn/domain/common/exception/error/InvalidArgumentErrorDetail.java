package gg.popn.domain.common.exception.error;

import lombok.Getter;

@Getter
public class InvalidArgumentErrorDetail implements ErrorDetail {
    private final String argumentName;
    private final String description;

    public InvalidArgumentErrorDetail(String argumentName, String description) {
        this.argumentName = argumentName;
        this.description = description;
    }

    @Override
    public String getReason() {
        return String.format("Argument %s is not valid. %s", argumentName, description);
    }
}
