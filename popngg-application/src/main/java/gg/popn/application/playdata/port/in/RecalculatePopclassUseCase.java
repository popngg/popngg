package gg.popn.application.playdata.port.in;

import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;

public interface RecalculatePopclassUseCase {
    PopclassRecalculationResult recalculate(String poptomoId);
}
