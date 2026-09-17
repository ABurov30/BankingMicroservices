package apigateway.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LocalCacheInvalidationPublisher {
  public static final String CHANNEL = "cache:l1-invalidation";
  private static final Logger log = LoggerFactory.getLogger(LocalCacheInvalidationPublisher.class);
  private final StringRedisTemplate redisTemplate;
  private final Counter pubSubPublishFailures;

  public LocalCacheInvalidationPublisher(StringRedisTemplate redisTemplate, MeterRegistry meters) {
    this.redisTemplate = redisTemplate;
    this.pubSubPublishFailures = meters.counter("cache.invalidation.pubsub.publish.failures");
  }

  public void publish(String key) {
    try {
      redisTemplate.convertAndSend(CHANNEL, key);
    } catch (DataAccessException exception) {
      pubSubPublishFailures.increment();
      log.warn("Redis Pub/Sub publish failed: channel={}, key={}", CHANNEL, key, exception);
      throw exception;
    }
  }
}
