package gg.popn.application.playdata.service;

import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.dto.query.FindUserRecordsQuery;
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
    public PlaydataQueryResults.UserRecords findUserRecords(
            String poptomoId, FindUserRecordsQuery query) {
        if (query.page() < 0) throw new IllegalArgumentException("page must not be negative");
        if (query.size() < 1 || query.size() > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (query.levelMin() != null && query.levelMax() != null
                && query.levelMin() > query.levelMax()) {
            throw new IllegalArgumentException("levelMin must not exceed levelMax");
        }
        if (query.scoreMin() != null && query.scoreMax() != null
                && query.scoreMin() > query.scoreMax()) {
            throw new IllegalArgumentException("scoreMin must not exceed scoreMax");
        }
        String sort = normalize(query.sort(), "LEVEL", "SCORE");
        String order = normalize(query.order(), "ASC", "DESC");
        return port.findUserRecords(poptomoId, new FindUserRecordsQuery(
                query.keyword(), query.version(), query.levelMin(), query.levelMax(),
                query.difficulties(), query.medals(), query.ranks(), query.scoreMin(),
                query.scoreMax(), sort, order, query.page(), query.size()));
    }

    @Override
    public PlaydataQueryResults.Progress findProgress(String poptomoId, String by) {
        return port.findProgress(poptomoId, normalize(by, "LEVEL", "DIFFICULTY"));
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
