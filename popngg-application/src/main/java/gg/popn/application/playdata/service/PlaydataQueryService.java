package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.application.playdata.port.out.PlaydataQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaydataQueryService implements PlaydataQueryUseCase {
    private final PlaydataQueryPort port;

    @Override
    public PlaydataQueryResults.UserPlaydata findUserPlaydata(String poptomoId) {
        return port.findUserPlaydata(poptomoId);
    }

    @Override
    public PlaydataQueryResults.Counts count(String poptomoId, String groupBy, String target) {
        String normalizedGroup = normalize(groupBy, "LEVEL", "DIFFICULTY");
        String normalizedTarget = normalize(target, "RANK", "MEDAL");
        return port.count(poptomoId, normalizedGroup, normalizedTarget);
    }

    @Override
    public PlaydataQueryResults.Popclass findPopclass(String poptomoId) {
        return port.findPopclass(poptomoId);
    }

    @Override
    public java.util.List<PlaydataQueryResults.ChartPlaydata> findLegacyPopclassTargets(
            String poptomoId) {
        return port.findLegacyPopclassTargets(poptomoId);
    }

    @Override
    public PlaydataQueryResults.ChartRankings findChartRankings(long chartId, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100.");
        }
        return port.findChartRankings(chartId, limit);
    }

    private static String normalize(String value, String first, String second) {
        if (value == null) throw new IllegalArgumentException("Query option is required.");
        String normalized = value.toUpperCase();
        if (!normalized.equals(first) && !normalized.equals(second)) {
            throw new IllegalArgumentException("Unsupported query option.");
        }
        return normalized;
    }
}
