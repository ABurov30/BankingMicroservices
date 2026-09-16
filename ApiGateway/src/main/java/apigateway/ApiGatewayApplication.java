package apigateway;

import apigateway.cache.CacheProperties;
import apigateway.config.AuthCookieProperties;
import apigateway.ratelimit.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
@EnableConfigurationProperties({
  RateLimitProperties.class,
  AuthCookieProperties.class,
  CacheProperties.class
})
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
