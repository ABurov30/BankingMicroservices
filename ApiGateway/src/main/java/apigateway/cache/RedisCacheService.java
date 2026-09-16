package apigateway.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
  private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
    try {
      String json = redisTemplate.opsForValue().get(key);

      if (json == null) {
        return Optional.empty();
      }

      return Optional.of(objectMapper.readValue(json, typeReference));
    } catch (JsonProcessingException exception) {
      log.warn("Removing unreadable cache value: key={}", key);
      evict(key);
      return Optional.empty();
    } catch (DataAccessException exception) {
      log.warn("Redis read failed; treating as cache miss: key={}", key, exception);
      return Optional.empty();
    }
  }

  public void put(String key, Object value, Duration ttl) {
    try {
      String json = objectMapper.writeValueAsString(value);
      redisTemplate.opsForValue().set(key, json, ttl);
    } catch (JsonProcessingException exception) {
      log.warn("Cannot serialize cache value: key={}", key, exception);
    } catch (DataAccessException exception) {
      log.warn("Redis write failed; skipping cache: key={}", key, exception);
    }
  }

  public void evict(String key) {
    try {
      redisTemplate.delete(key);
    } catch (DataAccessException exception) {
      log.warn("Redis eviction failed: key={}", key, exception);
    }
  }
}
