package apigateway.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
  private boolean enabled = true;
  private Limit defaultLimit = new Limit(120, 60);
  private Limit auth = new Limit(5, 60);
  private Limit transaction = new Limit(10, 60);

  public record Limit(int limit, int windowSeconds) {}
}
