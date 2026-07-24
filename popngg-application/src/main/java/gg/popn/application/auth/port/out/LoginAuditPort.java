package gg.popn.application.auth.port.out;

import gg.popn.domain.user.model.field.PoptomoId;

public interface LoginAuditPort {
    void recordSuccess(PoptomoId poptomoId);

    void recordInvalidCredentials(PoptomoId poptomoId);
}
