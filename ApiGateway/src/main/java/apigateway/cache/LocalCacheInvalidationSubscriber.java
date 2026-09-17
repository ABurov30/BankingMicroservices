package apigateway.cache;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalCacheInvalidationSubscriber implements MessageListener {
  private final LocalCacheInvalidationResolver resolver;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String key = new String(message.getBody(), StandardCharsets.UTF_8);
    resolver.invalidate(key);
  }
}
