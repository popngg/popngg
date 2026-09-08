package gg.popn.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.infra.db.adapter.PlaydataQueryJdbcAdapter;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedPlaydataQueryAdapterTest {
    private final PlaydataQueryJdbcAdapter delegate = mock(PlaydataQueryJdbcAdapter.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, String> stored = new HashMap<>();
    private CachedPlaydataQueryAdapter adapter;
    private final PlaydataQueryResults.Popclass actual = result(100);
    private final PlaydataQueryResults.Popclass potential = result(200);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
        org.mockito.Mockito.doAnswer(call -> {
            stored.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        when(delegate.findPopclass("0000")).thenReturn(actual);
        when(delegate.findPotentialPopclass("0000")).thenReturn(potential);
        adapter = new CachedPlaydataQueryAdapter(delegate, redis, mapper, true, 29);
    }

    @Test
    void cachesEachPopclassViewForThirtyMinutes() {
        assertThat(adapter.findPopclass("0000")).isEqualTo(actual);
        assertThat(adapter.findPopclass("0000")).isEqualTo(actual);
        assertThat(adapter.findPotentialPopclass("0000")).isEqualTo(potential);
        assertThat(adapter.findPotentialPopclass("0000")).isEqualTo(potential);

        verify(delegate).findPopclass("0000");
        verify(delegate).findPotentialPopclass("0000");
        verify(values, times(2)).set(anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(30)));
    }

    @Test
    void refreshEagerlyReplacesBothViews() {
        adapter.refresh("0000");

        assertThat(stored).hasSize(2);
        assertThat(adapter.findPopclass("0000")).isEqualTo(actual);
        assertThat(adapter.findPotentialPopclass("0000")).isEqualTo(potential);
        verify(delegate).findPopclass("0000");
        verify(delegate).findPotentialPopclass("0000");
    }

    @Test
    void redisOutageFallsBackToDatabase() {
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("offline"));

        assertThat(adapter.findPopclass("0000")).isEqualTo(actual);
        assertThat(adapter.findPopclass("0000")).isEqualTo(actual);
        verify(delegate, times(2)).findPopclass("0000");
    }

    @Test
    void disabledCacheDelegatesWithoutTouchingRedis() {
        var disabled = new CachedPlaydataQueryAdapter(delegate, redis, mapper, false, 29);

        assertThat(disabled.findPopclass("0000")).isEqualTo(actual);
        disabled.refresh("0000");
        org.mockito.Mockito.verifyNoInteractions(redis);
    }

    private static PlaydataQueryResults.Popclass result(int value) {
        return new PlaydataQueryResults.Popclass(
                "0000", "name", value, value, 0, List.of());
    }
}
