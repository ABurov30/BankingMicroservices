package apigateway.ratelimit;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimitService {
  private static final RedisScript<Long> SCRIPT =
      new DefaultRedisScript<>(
          """
          local current = redis.call('GET', KEYS[1])
          if current and tonumber(current) >= tonumber(ARGV[2]) then
            local ttl = redis.call('TTL', KEYS[1])
            if ttl < 0 then ttl = tonumber(ARGV[1]) end
            return ttl
          end

          current = redis.call('INCR', KEYS[1])
          if current == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[1])
          end

          return -1
          """,
          Long.class);

  private final StringRedisTemplate redisTemplate;

  public RedisRateLimitService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public RateLimitResult check(String key, int limit, int windowSeconds) {
    Long retryAfter =
        redisTemplate.execute(
            SCRIPT, List.of(key), String.valueOf(windowSeconds), String.valueOf(limit));

    if (retryAfter == null || retryAfter < 0) {
      return new RateLimitResult(true, 0);
    }

    return new RateLimitResult(false, retryAfter);
  }
}
