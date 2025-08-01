package gg.popn.domain.common.exception;

import gg.popn.domain.common.ResponseCode;
import gg.popn.domain.common.ResponseMessage;
import gg.popn.domain.common.exception.error.ChartNotFoundErrorDetail;

public class ChartNotFoundException extends BaseException {
    public ChartNotFoundException() {
        super(ResponseCode.NOT_FOUND, ResponseMessage.CHART_NOT_FOUND, new ChartNotFoundErrorDetail());
    }
}
