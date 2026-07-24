package gg.popn.http.common.response;

import gg.popn.domain.common.Code;
import gg.popn.domain.common.Message;

public interface Response<T> {
    public Code getCode();
    public Message getMessage();
    public T getData();
}
