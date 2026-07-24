package gg.popn.infra.db.adapter;

import gg.popn.application.auth.port.out.LoginAuditPort;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.infra.db.entity.LoginLogEntity;
import gg.popn.infra.db.jpa.LoginLogJpaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginAuditJpaAdapter implements LoginAuditPort {
    private final LoginLogJpaRepository repository;

    @Override
    public void recordSuccess(PoptomoId poptomoId) {
        save(poptomoId, "SUCCESS", null);
    }

    @Override
    public void recordInvalidCredentials(PoptomoId poptomoId) {
        save(poptomoId, "FAILURE", "INVALID_CREDENTIALS");
    }

    private void save(PoptomoId poptomoId, String status, String reason) {
        repository.save(LoginLogEntity.builder()
                .poptomoId(poptomoId.getValue())
                .status(status)
                .failureReason(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
