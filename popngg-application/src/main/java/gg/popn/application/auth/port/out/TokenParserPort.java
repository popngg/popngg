package gg.popn.application.auth.port.out;


import gg.popn.domain.user.model.AuthPrincipal;

import java.util.Optional;

public interface TokenParserPort {
    Optional<AuthPrincipal> parse(String token);
}