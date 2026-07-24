package gg.popn.application.user.port.out;


import gg.popn.application.user.dto.UserWithHashedPassword;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.Username;

import java.util.Optional;

public interface LoadUserPort {
    Optional<UserWithHashedPassword> loadByUsername(Username username);
    Optional<UserWithHashedPassword> loadByPoptomoId(PoptomoId poptomoId);

}
