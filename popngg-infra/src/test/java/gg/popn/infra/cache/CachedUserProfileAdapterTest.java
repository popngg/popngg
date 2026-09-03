package gg.popn.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.result.UserListResult;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.port.out.UserProfilePort;
import gg.popn.infra.db.adapter.UserProfileJpaAdapter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CachedUserProfileAdapterTest {
    private final UserProfileJpaAdapter delegate = mock(UserProfileJpaAdapter.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final Map<String, String> stored = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private CachedUserProfileAdapter adapter;
    private final FindUsersQuery query = new FindUsersQuery(null, FindUsersQuery.Sort.CLEAR_LEVEL,
            FindUsersQuery.Order.DESC, 0, 20);
    private final UserListResult result = new UserListResult(List.of(new UserListResult.Item(
            "0001", "name", null, "comment", 1, 100,
            List.of(new UserProfileResult.MedalSummary("clear", 49, 1, 20)),
            LocalDateTime.of(2026, 9, 3, 12, 34))), 0, 20, 1);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);
        when(values.get(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
        doAnswer(call -> {
            stored.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        when(delegate.findUsers(any())).thenReturn(result);
        adapter = new CachedUserProfileAdapter(delegate, redis, mapper, jdbc, true, 29);
    }

    @Test
    void roundTripsRecordsAndDatesAndUsesFiveMinuteTtl() {
        assertThat(adapter.findUsers(query)).isEqualTo(result);
        assertThat(adapter.findUsers(query)).isEqualTo(result);
        verify(delegate, times(1)).findUsers(query);
        verify(values).set(anyString(), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void committedRevisionChangeNeverReusesOldPublicResponse() {
        adapter.findUsers(query);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(2L);
        var hidden = new UserListResult(List.of(), 0, 20, 0);
        when(delegate.findUsers(query)).thenReturn(hidden);
        assertThat(adapter.findUsers(query)).isEqualTo(hidden);
        assertThat(adapter.findUsers(query)).isEqualTo(hidden);
        verify(delegate, times(2)).findUsers(query);
        assertThat(stored).hasSize(2);
    }

    @Test
    void inFlightOldReadCanOnlyPopulateOldRevisionKey() {
        when(delegate.findUsers(query)).thenAnswer(call -> {
            when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(2L);
            return result;
        });
        adapter.findUsers(query);
        var fresh = new UserListResult(List.of(), 0, 20, 0);
        when(delegate.findUsers(query)).thenReturn(fresh);
        assertThat(adapter.findUsers(query)).isEqualTo(fresh);
        verify(delegate, times(2)).findUsers(query);
    }

    @Test
    void redisOutageFallsBackAndBacksOff() {
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("offline"));
        assertThat(adapter.findUsers(query)).isEqualTo(result);
        assertThat(adapter.findUsers(query)).isEqualTo(result);
        verify(values, times(1)).get(anyString());
        verify(delegate, times(2)).findUsers(query);
    }

    @Test
    void failedCacheWriteDoesNotFailSuccessfulDbRead() {
        doThrow(new RedisConnectionFailureException("offline"))
                .when(values).set(anyString(), anyString(), any(Duration.class));
        assertThat(adapter.findUsers(query)).isEqualTo(result);
    }

    @Test
    void malformedCacheFallsBack() {
        when(values.get(anyString())).thenReturn("invalid json");
        assertThat(adapter.findUsers(query)).isEqualTo(result);
    }

    @Test
    void dbFailureIsNotSwallowedOrRetried() {
        when(delegate.findUsers(query)).thenThrow(new IllegalStateException("database failed"));
        assertThatThrownBy(() -> adapter.findUsers(query)).isInstanceOf(IllegalStateException.class);
        verify(delegate).findUsers(query);
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void doesNotCacheSearchLaterPagesOrPersonalProfiles() {
        adapter.findUsers(new FindUsersQuery("name", query.sort(), query.order(), 0, 20));
        adapter.findUsers(new FindUsersQuery(null, query.sort(), query.order(), 1, 20));
        adapter.findByPoptomoId("0001");
        verifyNoInteractions(redis, jdbc);
    }

    @Test
    void disabledCacheNeedsNoRedisOrRevisionLookup() {
        var disabled = new CachedUserProfileAdapter(delegate, redis, mapper, jdbc, false, 29);
        assertThat(disabled.findUsers(query)).isEqualTo(result);
        verifyNoInteractions(redis, jdbc);
    }

    @Test
    void distinguishesSortOrderSizeAndRankingNamespace() {
        adapter.findUsers(query);
        adapter.findUsers(new FindUsersQuery(null, FindUsersQuery.Sort.RANK, query.order(), 0, 20));
        adapter.findUsers(new FindUsersQuery(null, query.sort(), FindUsersQuery.Order.ASC, 0, 20));
        adapter.findUsers(new FindUsersQuery(null, query.sort(), query.order(), 0, 10));
        var rankingQuery = new UserRankingQuery(UserRankingQuery.Sort.DISPLAY_POPCLASS, 0, 20);
        var ranking = new UserProfilePort.RankingPage(List.of(), 0);
        when(delegate.findRankings(rankingQuery)).thenReturn(ranking);
        assertThat(adapter.findRankings(rankingQuery)).isEqualTo(ranking);
        assertThat(adapter.findRankings(rankingQuery)).isEqualTo(ranking);
        verify(delegate).findRankings(rankingQuery);
        assertThat(stored).hasSize(5);
    }

    @Test
    void neverCachesDataFromAnUncommittedWriteTransaction() {
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThat(adapter.findUsers(query)).isEqualTo(result);
            verifyNoInteractions(redis, jdbc);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void newProcessDoesNotReusePreviousBinaryCache() {
        adapter.findUsers(query);
        var restarted = new CachedUserProfileAdapter(delegate, redis, mapper, jdbc, true, 29);
        restarted.findUsers(query);
        verify(delegate, times(2)).findUsers(query);
    }
}
