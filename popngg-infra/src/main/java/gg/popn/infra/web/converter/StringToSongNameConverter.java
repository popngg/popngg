package gg.popn.infra.web.converter;

import gg.popn.domain.chart.model.field.SongName;
import org.springframework.core.convert.converter.Converter;

public class StringToSongNameConverter implements Converter<String, SongName> {

    @Override
    public SongName convert(String source) {
        return SongName.of(source);
    }
}
