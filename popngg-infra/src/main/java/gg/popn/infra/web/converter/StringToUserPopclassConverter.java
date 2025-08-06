package gg.popn.infra.web.converter;

import gg.popn.domain.user.model.field.UserPopclass;
import org.springframework.core.convert.converter.Converter;

public class StringToUserPopclassConverter implements Converter<String, UserPopclass> {

    @Override
    public UserPopclass convert(String source) {
        return UserPopclass.of(Integer.parseInt(source));
    }
}
