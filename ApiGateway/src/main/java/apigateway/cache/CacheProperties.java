package apigateway.cache;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "account.overview.cache")
public class CacheProperties {
  private boolean enabled;
  private Duration l1Ttl;
  private Duration l2Ttl;
  private int l1MaxSize;
}
