package gg.popn.infra.web.converter;

import gg.popn.domain.user.model.field.Username;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToUsernameConverter implements Converter<String, Username> {

    @Override
    public Username convert(String source) {
        return Username.of(source);
    }
}
