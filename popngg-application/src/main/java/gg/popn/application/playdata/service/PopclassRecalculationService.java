package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;
import gg.popn.application.playdata.port.in.RecalculatePopclassUseCase;
import gg.popn.application.playdata.port.out.PopclassRecalculationPort;
import gg.popn.application.playdata.port.out.PopclassCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopclassRecalculationService implements RecalculatePopclassUseCase {
    private final PopclassRecalculationPort port;
    private final PopclassCachePort cachePort;

    @Override
    public PopclassRecalculationResult recalculate(String poptomoId) {
        if (poptomoId == null || poptomoId.isBlank()) {
            throw new IllegalArgumentException("poptomoId is required.");
        }
        var result = port.recalculate(poptomoId);
        cachePort.refresh(poptomoId);
        return result;
    }
}
