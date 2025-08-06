package gg.popn.infra.web.converter;

import gg.popn.domain.user.model.field.UserRole;
import org.springframework.core.convert.converter.Converter;

public class StringToUserRoleConverter implements Converter<String, UserRole> {

    @Override
    public UserRole convert(String source) {
        return UserRole.of(source);
    }
}
