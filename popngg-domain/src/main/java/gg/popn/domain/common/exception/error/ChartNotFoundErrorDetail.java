package gg.popn.domain.common.exception.error;

import lombok.Getter;

@Getter
public class ChartNotFoundErrorDetail implements ErrorDetail {

    @Override
    public String getReason() {
        return "Chart is not found.";
    }
}
