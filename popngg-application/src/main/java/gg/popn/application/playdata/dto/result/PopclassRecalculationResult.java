package gg.popn.application.playdata.dto.result;

public record PopclassRecalculationResult(
        String poptomoId,
        int legacyPopclass,
        int displayPopclass,
        int potentialPopclass,
        int newPopclassScale,
        int currentVersionTargets,
        int oldVersionTargets
) {
}
