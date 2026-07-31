package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;
import gg.popn.application.playdata.port.in.RecalculatePopclassUseCase;
import gg.popn.application.playdata.port.out.PopclassRecalculationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopclassRecalculationService implements RecalculatePopclassUseCase {
    private final PopclassRecalculationPort port;

    @Override
    public PopclassRecalculationResult recalculate(String poptomoId) {
        if (poptomoId == null || poptomoId.isBlank()) {
            throw new IllegalArgumentException("poptomoId is required.");
        }
        return port.recalculate(poptomoId);
    }
}
