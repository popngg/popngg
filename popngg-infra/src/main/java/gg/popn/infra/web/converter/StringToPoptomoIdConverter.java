package gg.popn.infra.web.converter;

import gg.popn.domain.user.model.field.PoptomoId;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;

public class StringToPoptomoIdConverter implements Converter<String, PoptomoId> {

    @Override
    public PoptomoId convert(@NonNull String source) {
        return PoptomoId.of(source);
    }
}
