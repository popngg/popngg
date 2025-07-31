package gg.popn.domain.common;

import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;

public interface Message extends Serializable {
    @JsonValue
    public String getValue();

    public String getKey();
}
