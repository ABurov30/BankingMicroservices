package accountservice.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountClockConfig {
  @Bean
  public Clock accountClock() {
    return Clock.systemUTC();
  }
}
