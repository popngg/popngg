package gg.popn.infra.web.converter;

import gg.popn.domain.chart.model.field.IsUpper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToIsUpperConverter implements Converter<String, IsUpper> {

    @Override
    public IsUpper convert(String source) {
        return IsUpper.of(Integer.parseInt(source));
    }
}
