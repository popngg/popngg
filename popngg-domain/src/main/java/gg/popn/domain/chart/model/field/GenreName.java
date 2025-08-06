package gg.popn.domain.chart.model.field;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GenreName {

    String genreName;

    public static GenreName of(String genreName) {
        if (genreName == null) {
            return null;
        }

        return GenreName.builder()
                .genreName(genreName)
                .build();
    }

    @JsonValue
    public String getValue() {
        return genreName;
    }
}
