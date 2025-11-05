package gg.popn.application.auth.port.out;

import gg.popn.application.auth.model.AuthPrincipal;
import java.util.Optional;

public interface CurrentPrincipalPort {
    Optional<AuthPrincipal> get();
}
