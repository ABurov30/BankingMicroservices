package apigateway.config;

import apigateway.websocket.JwtHandshakeHandler;
import apigateway.websocket.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtHandshakeHandler jwtHandshakeHandler;
  private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .addInterceptors(jwtHandshakeInterceptor)
        .setHandshakeHandler(jwtHandshakeHandler)
        .setAllowedOriginPatterns("http://localhost:*", "https://buro-bank.ru");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/queue");
    registry.setUserDestinationPrefix("/user");
  }
}
