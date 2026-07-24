package gg.popn.application.auth.service;

import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.dto.command.LoginCommand;
import gg.popn.application.auth.dto.response.LoginResult;
import gg.popn.application.auth.port.out.LoginAuditPort;
import gg.popn.application.auth.port.out.PasswordVerificationPort;
import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.application.user.port.out.LoadUserPort;

import gg.popn.domain.user.model.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateUserService implements AuthenticateUserUseCase {
    private final LoadUserPort loadUserPort;
    private final PasswordVerificationPort passwordVerifier;
    private final TokenPort tokenIssuer;
    private final LoginAuditPort loginAudit;

    @Override
    public LoginResult login(LoginCommand cmd) {
        var u = loadUserPort.loadByPoptomoId(cmd.poptomoId())
                .orElse(null);

        if (u == null || !passwordVerifier.matches(cmd.password().getValue(), u.passwordHash())) {
            loginAudit.recordInvalidCredentials(cmd.poptomoId());
            throw new BadCredentialsException("Invalid credentials");
        }

        var user = u.user();
        var principal = AuthPrincipal.of(user.getPoptomoId(), user.getRole());
        var token = tokenIssuer.issueAccessToken(principal);
        loginAudit.recordSuccess(cmd.poptomoId());
        return new LoginResult(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                principal.getUserRole().getValue(),
                new LoginResult.UserProfileSummary(
                        user.getPoptomoId().getValue(),
                        user.getUsername().getValue()));
    }
}
