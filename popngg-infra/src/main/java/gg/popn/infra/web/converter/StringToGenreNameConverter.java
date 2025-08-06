package gg.popn.infra.web.converter;

import gg.popn.domain.chart.model.field.GenreName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToGenreNameConverter implements Converter<String, GenreName> {

    @Override
    public GenreName convert(String source) {
        return GenreName.of(source);
    }
}
