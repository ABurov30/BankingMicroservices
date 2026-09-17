package apigateway.websocket;

import apigateway.security.AccessStateRedisService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtDecoder jwtDecoder;
  private final AccessStateRedisService accessStateRedisService;

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    if (!(request instanceof ServletServerHttpRequest servletRequest)) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }

    HttpServletRequest httpRequest = servletRequest.getServletRequest();

    String accessToken =
        Arrays.stream(httpRequest.getCookies() == null ? new Cookie[0] : httpRequest.getCookies())
            .filter(cookie -> "at".equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);

    if (accessToken == null || accessToken.isBlank()) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }

    try {
      var jwt = jwtDecoder.decode(accessToken);
      if (!accessStateRedisService.isActive(UUID.fromString(jwt.getSubject()))) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
      }
      return true;
    } catch (JwtException ex) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
