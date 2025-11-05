package gg.popn.infra.db.adapter;

import gg.popn.application.user.port.out.LoadUserPort;
import gg.popn.domain.user.model.User;
import gg.popn.domain.user.model.field.*;
import gg.popn.infra.db.entity.UserEntity;
import gg.popn.infra.db.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements LoadUserPort {
    private final UserJpaRepository repo;

    @Override
    public Optional<UserWithSecret> loadByUsername(Username username) {
        return repo.findByUserName(username.getValue()).map(this::toUserWithSecret);
    }

    @Override
    public Optional<UserWithSecret> loadById(Long id) {
        return repo.findById(id).map(this::toUserWithSecret);
    }

    private UserWithSecret toUserWithSecret(UserEntity e) {
        User domain = User.builder()
                .username(Username.of(e.getUserName()))
                .poptomoId(PoptomoId.of(e.getPoptomoId()))
                .role(UserRole.from(e.getRole()))
                .build();
        return new UserWithSecret(domain, e.getPassword());
    }
}
