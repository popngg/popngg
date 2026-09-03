package gg.popn.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.user.dto.query.FindUsersQuery;
import gg.popn.application.user.dto.result.UserListResult;
import gg.popn.infra.db.adapter.UserProfileJpaAdapter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers(disabledWithoutDocker = true)
class UserDirectoryRedisIntegrationTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @Test
    void cachesWithExpiryAndRebuildsAfterEviction() {
        var connection = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connection.afterPropertiesSet();
        try {
            var redis = new StringRedisTemplate(connection);
            var delegate = mock(UserProfileJpaAdapter.class);
            var jdbc = mock(JdbcTemplate.class);
            when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);
            var query = new FindUsersQuery(null, FindUsersQuery.Sort.CLEAR_LEVEL, FindUsersQuery.Order.DESC, 0, 20);
            var result = new UserListResult(List.of(), 0, 20, 0);
            when(delegate.findUsers(query)).thenReturn(result);
            var cache = new CachedUserProfileAdapter(delegate, redis, new ObjectMapper().findAndRegisterModules(), jdbc, true, 29);
            assertThat(cache.findUsers(query)).isEqualTo(result);
            assertThat(cache.findUsers(query)).isEqualTo(result);
            verify(delegate).findUsers(query);
            var keys = redis.keys("popngg:users:*");
            assertThat(keys).hasSize(1);
            assertThat(redis.getExpire(keys.iterator().next(), TimeUnit.SECONDS)).isBetween(1L, 300L);
            redis.delete(keys);
            assertThat(cache.findUsers(query)).isEqualTo(result);
            verify(delegate, times(2)).findUsers(query);
        } finally {
            connection.destroy();
        }
    }
}
