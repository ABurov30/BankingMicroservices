package apigateway.config;

import apigateway.cache.LocalCacheInvalidationPublisher;
import apigateway.cache.LocalCacheInvalidationSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisPubSubConfig {
  private final RedisConnectionFactory connectionFactory;
  private final LocalCacheInvalidationSubscriber subscriber;

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer() {
    var container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(
        subscriber, new ChannelTopic(LocalCacheInvalidationPublisher.CHANNEL));
    return container;
  }
}
