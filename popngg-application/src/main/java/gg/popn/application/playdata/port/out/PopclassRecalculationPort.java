package gg.popn.application.playdata.port.out;

import gg.popn.application.playdata.dto.result.PopclassRecalculationResult;

public interface PopclassRecalculationPort {
    PopclassRecalculationResult recalculate(String poptomoId);
}
