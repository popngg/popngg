package gg.popn.domain.common.exception;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.common.exception.error.InvalidArgumentErrorDetail;

public class InvalidArgumentException extends BaseException {
    public InvalidArgumentException(String argumentName) {
        super(ResponseCode.BAD_REQUEST, ResponseMessage.BAD_REQUEST, new InvalidArgumentErrorDetail(argumentName, null));
    }

    public InvalidArgumentException(String argumentName, String description) {
        super(ResponseCode.BAD_REQUEST, ResponseMessage.BAD_REQUEST, new InvalidArgumentErrorDetail(argumentName, description));
    }
}
