package gg.popn.domain.chart.application.service.validator;

import gg.popn.domain.common.exception.InvalidArgumentException;
import gg.popn.domain.common.validator.Validator;
import org.springframework.stereotype.Component;

@Component
public class ChartLevelValidator implements Validator<Integer> {
    @Override
    public void validate(Integer level) {
        if (level == null) {
            throw new InvalidArgumentException("level", "It should not be empty.");
        }

        if (level <= 0) {
            throw new InvalidArgumentException("level", "It should be greater than 0.");
        }

        if (level >= 51) {
            throw new InvalidArgumentException("level", "It should be less than 51.");
        }
    }
}
