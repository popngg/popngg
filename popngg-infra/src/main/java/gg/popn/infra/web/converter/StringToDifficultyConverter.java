package gg.popn.infra.web.converter;

import gg.popn.domain.chart.model.field.Difficulty;
import org.springframework.core.convert.converter.Converter;

public class StringToDifficultyConverter implements Converter<String, Difficulty> {

    @Override
    public Difficulty convert(String source) {
        return Difficulty.of(Integer.parseInt(source));
    }
}
