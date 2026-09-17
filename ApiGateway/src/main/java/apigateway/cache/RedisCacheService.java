package apigateway.cache;

import cache.BaseRedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService extends BaseRedisCacheService<CacheProperties> {
  private final Counter l2EvictionFailures;

  protected RedisCacheService(
      StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      CacheProperties cacheProperties,
      MeterRegistry meters) {
    super(
        redisTemplate,
        objectMapper,
        cacheProperties,
        meters.counter("cache.invalidation.l2.evictions"));
    this.l2EvictionFailures = meters.counter("cache.invalidation.l2.eviction.failures");
  }

  @Override
  public void evict(String key) {
    try {
      super.evict(key);
    } catch (DataAccessException exception) {
      l2EvictionFailures.increment();
      throw exception;
    }
  }
}
