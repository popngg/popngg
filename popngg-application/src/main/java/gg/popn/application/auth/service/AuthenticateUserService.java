package gg.popn.application.auth.service;

import gg.popn.application.auth.port.in.AuthenticateUserUseCase;
import gg.popn.application.auth.port.in.command.LoginCommand;
import gg.popn.application.auth.port.in.result.AuthResult;
import gg.popn.application.auth.port.out.PasswordHasherPort;
import gg.popn.application.auth.port.out.TokenIssuerPort;
import gg.popn.application.user.port.out.LoadUserPort;

import gg.popn.domain.user.model.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthenticateUserService implements AuthenticateUserUseCase {
    private final LoadUserPort loadUserPort;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;

    @Override
    public AuthResult login(LoginCommand cmd) {
        // 1) 사용자 + 해시 로드 (식별자 = PoptomoId)
        var u = loadUserPort.loadByPoptomoId(cmd.poptomoId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // 2) 패스워드 검증 (VO에서 원문 꺼내서 비교)
        if (!passwordHasher.matches(cmd.password().getValue(), u.passwordHash())) {
            throw new BadCredentialsException("Bad credentials");
        }

        // 3) 인증 주체 생성 (PoptomoId + Role)
        var user = u.user();
        var principal = AuthPrincipal.of(user.getPoptomoId(), user.getRole());

        // 4) 액세스 토큰 발급
        var token = tokenIssuer.issueAccessToken(principal, Duration.ofHours(12));
        return new AuthResult(token, principal);
    }

    @Override
    public AuthResult loginWithoutHash(LoginCommand cmd) {
        // 1) 사용자 + 해시 로드 (식별자 = PoptomoId)
        var u = loadUserPort.loadByPoptomoId(cmd.poptomoId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // 2) 패스워드 검증 (VO에서 원문 꺼내서 비교)
        if (!u.passwordHash().equals(cmd.password().getValue())) {
            throw new BadCredentialsException("Bad credentials");
        }
        // 3) 인증 주체 생성 (PoptomoId + Role)
        var user = u.user();
        var principal = AuthPrincipal.of(user.getPoptomoId(), user.getRole());

        // 4) 액세스 토큰 발급
        var token = tokenIssuer.issueAccessToken(principal, Duration.ofHours(12));
        return new AuthResult(token, principal);
    }

}
