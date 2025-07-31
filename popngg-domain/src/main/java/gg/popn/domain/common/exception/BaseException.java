package gg.popn.domain.common.exception;

import gg.popn.domain.common.Code;
import gg.popn.domain.common.Message;
import gg.popn.domain.common.exception.error.ErrorDetail;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BaseException extends RuntimeException {
    private final Code code;
    private final Message errorMessage;
    private final ErrorDetail errorDetail;

    public BaseException(Code code, Message errorMessage) {
        this(code, errorMessage, null, null);
    }

    public BaseException(Code code, Message errorMessage, ErrorDetail errorDetail) {
        super(errorMessage.getValue());
        this.code = code;
        this.errorMessage = errorMessage;
        this.errorDetail = errorDetail;
    }

    public BaseException(Code code, Message errorMessage, Throwable t) {
        this(code, errorMessage, null, t);
    }

    public BaseException(Code code, Message errorMessage, ErrorDetail errorDetail, Throwable t) {
        super(errorMessage.getValue(), t);
        this.code = code;
        this.errorMessage = errorMessage;
        this.errorDetail = errorDetail;
    }
}
