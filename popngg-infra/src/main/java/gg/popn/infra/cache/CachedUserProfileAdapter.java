package gg.popn.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.result.UserListResult;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.port.out.UserProfilePort;
import gg.popn.infra.db.adapter.UserProfileJpaAdapter;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

/** Only public directory responses are cached, never an authenticated user's profile. */
@Component
@Primary
public class CachedUserProfileAdapter implements UserProfilePort {
    private static final Logger log = LoggerFactory.getLogger(CachedUserProfileAdapter.class);
    private final UserProfileJpaAdapter delegate;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final String namespace;
    private volatile long retryAfterNanos;

    public CachedUserProfileAdapter(UserProfileJpaAdapter delegate, StringRedisTemplate redis,
            ObjectMapper mapper, JdbcTemplate jdbc,
            @Value("${popngg.user-directory.cache-enabled:false}") boolean enabled,
            @Value("${popngg.game.current-version:29}") int currentVersion) {
        this.delegate = delegate;
        this.redis = redis;
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.enabled = enabled;
        // A new binary/start never reuses results written by an older application.
        this.namespace = "popngg:users:v1:" + currentVersion + ":" + UUID.randomUUID() + ":";
    }

    @Override
    public Optional<UserProfileResult> findByPoptomoId(String id) {
        return delegate.findByPoptomoId(id);
    }

    @Override
    @Transactional
    public UserProfileResult update(UpdateUserProfileCommand command) {
        return delegate.update(command);
    }

    @Override
    @Transactional(readOnly = true)
    public RankingPage findRankings(UserRankingQuery query) {
        if (query.page() != 0) return delegate.findRankings(query);
        return cached("rankings:" + query.sort() + ":" + query.size(), RankingPage.class,
                () -> delegate.findRankings(query));
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResult findUsers(FindUsersQuery query) {
        if (query.page() != 0 || (query.keyword() != null && !query.keyword().isBlank())) {
            return delegate.findUsers(query);
        }
        return cached("list:" + query.sort() + ":" + query.order() + ":" + query.size(),
                UserListResult.class, () -> delegate.findUsers(query));
    }

    private <T> T cached(String suffix, Class<T> type, Supplier<T> loader) {
        if (!enabled || System.nanoTime() < retryAfterNanos) return loader.get();
        // Never publish uncommitted data under a revision that could roll back and
        // later be reused by another writer. Normal list calls are read-only.
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return loader.get();
        }
        // Same DB snapshot as the underlying read. An in-flight old read can only
        // populate an old revision key; Redis failure cannot lose an invalidation.
        Long revision = jdbc.queryForObject(
                "SELECT revision FROM user_directory_revision WHERE id = 1", Long.class);
        String key = namespace + revision + ":" + suffix;
        try {
            String json = redis.opsForValue().get(key);
            if (json != null) return mapper.readValue(json, type);
        } catch (RuntimeException | JsonProcessingException exception) {
            unavailable(exception);
            return loader.get();
        }
        T result = loader.get(); // DB errors must propagate, not be treated as cache errors.
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(result), Duration.ofMinutes(5));
        } catch (RuntimeException | JsonProcessingException exception) {
            unavailable(exception);
        }
        return result;
    }

    private synchronized void unavailable(Exception exception) {
        if (System.nanoTime() >= retryAfterNanos) {
            log.warn("User directory cache unavailable; using DB for 30 seconds ({})",
                    exception.getClass().getSimpleName());
        }
        retryAfterNanos = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    }
}
