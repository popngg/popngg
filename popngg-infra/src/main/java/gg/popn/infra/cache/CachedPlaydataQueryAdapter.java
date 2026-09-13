package gg.popn.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.playdata.dto.query.FindUserRecordsQuery;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.out.PlaydataQueryPort;
import gg.popn.application.playdata.port.out.PopclassCachePort;
import gg.popn.infra.db.adapter.PlaydataQueryJdbcAdapter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Caches the two current user popclass tables and eagerly refreshes them after writes. */
@Component
@Primary
public class CachedPlaydataQueryAdapter implements PlaydataQueryPort, PopclassCachePort {
    private static final Logger log = LoggerFactory.getLogger(CachedPlaydataQueryAdapter.class);
    private static final Duration TTL = Duration.ofMinutes(30);

    private final PlaydataQueryJdbcAdapter delegate;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final String namespace;
    private volatile long retryAfterNanos;

    public CachedPlaydataQueryAdapter(PlaydataQueryJdbcAdapter delegate,
            StringRedisTemplate redis, ObjectMapper mapper,
            @Value("${popngg.popclass.cache-enabled:true}") boolean enabled,
            @Value("${popngg.game.current-version:29}") int currentVersion) {
        this.delegate = delegate;
        this.redis = redis;
        this.mapper = mapper;
        this.enabled = enabled;
        this.namespace = "popngg:popclass:v1:" + currentVersion + ":" + UUID.randomUUID() + ":";
    }

    @Override
    public PlaydataQueryResults.Popclass findPopclass(String poptomoId) {
        return cached(key(poptomoId, "actual"), () -> delegate.findPopclass(poptomoId));
    }

    @Override
    public PlaydataQueryResults.Popclass findPotentialPopclass(String poptomoId) {
        return cached(key(poptomoId, "potential"), () -> delegate.findPotentialPopclass(poptomoId));
    }

    @Override
    public void refresh(String poptomoId) {
        if (!enabled || System.nanoTime() < retryAfterNanos) return;
        warm(key(poptomoId, "actual"), () -> delegate.findPopclass(poptomoId));
        warm(key(poptomoId, "potential"), () -> delegate.findPotentialPopclass(poptomoId));
    }

    private PlaydataQueryResults.Popclass cached(
            String key, Supplier<PlaydataQueryResults.Popclass> loader) {
        if (!enabled || System.nanoTime() < retryAfterNanos) return loader.get();
        try {
            String json = redis.opsForValue().get(key);
            if (json != null) return mapper.readValue(json, PlaydataQueryResults.Popclass.class);
        } catch (RuntimeException | JsonProcessingException exception) {
            unavailable(exception);
            return loader.get();
        }
        PlaydataQueryResults.Popclass result = loader.get();
        store(key, result);
        return result;
    }

    private void warm(String key, Supplier<PlaydataQueryResults.Popclass> loader) {
        try {
            store(key, loader.get());
        } catch (RuntimeException exception) {
            log.warn("Popclass cache warm failed; the next read will use the database ({})",
                    exception.getClass().getSimpleName());
        }
    }

    private void store(String key, PlaydataQueryResults.Popclass value) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), TTL);
        } catch (RuntimeException | JsonProcessingException exception) {
            unavailable(exception);
        }
    }

    private String key(String poptomoId, String view) {
        return namespace + poptomoId + ":" + view;
    }

    private synchronized void unavailable(Exception exception) {
        if (System.nanoTime() >= retryAfterNanos) {
            log.warn("Popclass cache unavailable; using DB for 30 seconds ({})",
                    exception.getClass().getSimpleName());
        }
        retryAfterNanos = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    }

    @Override
    public PlaydataQueryResults.UserPlaydata findUserPlaydata(String poptomoId) {
        return delegate.findUserPlaydata(poptomoId);
    }

    @Override
    public PlaydataQueryResults.Counts count(String poptomoId, String groupBy, String target) {
        return delegate.count(poptomoId, groupBy, target);
    }

    @Override
    public PlaydataQueryResults.UserRecords findUserRecords(
            String poptomoId, FindUserRecordsQuery query) {
        return delegate.findUserRecords(poptomoId, query);
    }

    @Override
    public PlaydataQueryResults.Progress findProgress(String poptomoId, String by) {
        return delegate.findProgress(poptomoId, by);
    }

    @Override
    public List<PlaydataQueryResults.ChartPlaydata> findLegacyPopclassTargets(String poptomoId) {
        return delegate.findLegacyPopclassTargets(poptomoId);
    }

    @Override
    public PlaydataQueryResults.ChartRankingsPage findChartRankings(
            String songHash, int difficulty, String axis, int page, int size) {
        return delegate.findChartRankings(songHash, difficulty, axis, page, size);
    }

    @Override
    public PlaydataQueryResults.ChartRankings findChartRankings(long chartId, int limit) {
        return delegate.findChartRankings(chartId, limit);
    }
}
