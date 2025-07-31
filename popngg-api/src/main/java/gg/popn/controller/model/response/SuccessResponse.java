package gg.popn.controller.model.response;

import gg.popn.domain.common.Code;
import gg.popn.domain.common.Message;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SuccessResponse<T> implements Response<T> {
    private final Code code;
    private final Message message;
    private final T data;

    public SuccessResponse(Code code, Message message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
