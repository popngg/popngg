package gg.popn.domain.history.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validatable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Rank implements Validatable {

    Integer rank;

    @JsonCreator
    public static Rank of(Integer rank) {
        if (rank == null) {
            return null;
        }

        return Validatable.createAndValidate(() -> Rank.builder()
                .rank(rank)
                .build());
    }

    @JsonValue
    public Integer getValue() {
        return rank;
    }

    @Override
    public void validate() {
        if (rank == null) {
            throw new InvalidArgumentException("rank", "It should not be empty.");
        }
    }
}