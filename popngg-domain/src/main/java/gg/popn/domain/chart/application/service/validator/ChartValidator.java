package gg.popn.domain.chart.application.service.validator;

import gg.popn.domain.common.validator.Validatable;
import gg.popn.domain.common.validator.Validator;
import org.springframework.stereotype.Component;

@Component
public class ChartValidator implements Validator {
    @Override
    public void validate(Validatable... objects) {
        for (Validatable object : objects) {
            object.validate();
        }
    }
}
