package gg.popn.domain.common;

import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;

public interface Code extends Serializable {
    @JsonValue
    public String getValue();

    default int getStatusCode() {
        return 500;
    }
}
