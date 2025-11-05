package gg.popn.application.auth.service;

import gg.popn.application.auth.model.AuthPrincipal;
import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.TokenIssuerPort;
import gg.popn.application.user.port.out.LoadUserPort;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthenticateUserService implements AuthenticateUserUseCase {
    private final LoadUserPort loadUserPort;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;

    @Override
    public AuthResult login(Username username, Password password) {
        var u = loadUserPort.loadByUsername(Username.of(username))
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("User not found"));

        if (!passwordHasher.matches(password, u.passwordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Bad credentials");
        }

        // ✔ PoptomoId + UserRole만으로 AuthPrincipal 생성
        var user = u.user();
        var principal = new AuthPrincipal(user.getPoptomoId(), user.getRole());

        // ✔ 토큰 발급은 principal을 그대로 사용 (subject 등을 나중에 infra에서 poptomoId로 씀)
        var token = tokenIssuer.issueAccessToken(principal, Duration.ofHours(12));

        return new AuthResult(token, principal);
    }
}