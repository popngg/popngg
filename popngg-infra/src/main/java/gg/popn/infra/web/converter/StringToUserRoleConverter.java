package gg.popn.infra.web.converter;

import gg.popn.domain.user.model.field.UserRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToUserRoleConverter implements Converter<String, UserRole> {

    @Override
    public UserRole convert(String source) {
        return UserRole.of(source);
    }
}
