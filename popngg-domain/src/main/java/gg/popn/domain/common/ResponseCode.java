package gg.popn.domain.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum ResponseCode implements Code {
    SUCCESS(HttpStatus.OK.value()),
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value()),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value()),
    FORBIDDEN(HttpStatus.FORBIDDEN.value()),
    NOT_FOUND(HttpStatus.NOT_FOUND.value()),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ;

    @Getter
    private final int statusCode;

    ResponseCode() {
        this.statusCode = 500;
    }

    ResponseCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String getValue() {
        return this.name();
    }
}
