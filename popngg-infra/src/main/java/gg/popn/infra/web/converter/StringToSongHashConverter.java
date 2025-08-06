package gg.popn.infra.web.converter;

import gg.popn.domain.chart.model.field.SongHash;
import org.springframework.core.convert.converter.Converter;

public class StringToSongHashConverter implements Converter<String, SongHash> {

    @Override
    public SongHash convert(String source) {
        return SongHash.of(source);
    }
}
