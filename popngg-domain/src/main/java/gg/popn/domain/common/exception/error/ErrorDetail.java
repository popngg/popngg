package gg.popn.domain.common.exception.error;

import java.io.Serializable;

public interface ErrorDetail extends Serializable {
    public String getReason();
}
