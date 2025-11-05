package gg.popn.application.user.port.out;

import gg.popn.domain.user.model.User;
import gg.popn.domain.user.model.field.Username;

import java.util.Optional;

public interface LoadUserPort {
    Optional<UserWithSecret> loadByUsername(Username username);
    Optional<UserWithSecret> loadById(Long id);

    record UserWithSecret(User user, String passwordHash) {}
}
