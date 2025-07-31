package gg.popn.domain.common;

import lombok.Getter;

public enum ResponseMessage implements Message {
    SUCCESS("Success", "The request is successful."),
    BAD_REQUEST("BadRequest", "The request is invalid."),
    INTERNAL_SERVER_ERROR("InternalServerError", "An internal server error has occurred."),
    ;

    @Getter
    private final String key;

    private final String defaultMessage;

    ResponseMessage(String key) {
        this.key = key;
        this.defaultMessage = "";
    }

    ResponseMessage(String key, String defaultMessage) {
        this.key = key;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getValue() {
        return this.defaultMessage;
    }
}
