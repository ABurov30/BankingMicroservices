package apigateway.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

public class RateLimitFilter extends OncePerRequestFilter {
  private final RedisRateLimitService rateLimitService;
  private final RateLimitProperties properties;
  private final JsonMapper jsonMapper;

  public RateLimitFilter(
      RedisRateLimitService rateLimitService,
      RateLimitProperties properties,
      JsonMapper jsonMapper) {
    this.rateLimitService = rateLimitService;
    this.properties = properties;
    this.jsonMapper = jsonMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!properties.isEnabled() || shouldSkip(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    RateLimitProperties.Limit limit = resolveLimit(request);
    String key = resolveKey(request);

    RateLimitResult result = rateLimitService.check(key, limit.limit(), limit.windowSeconds());

    if (result.allowed()) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
    response.setContentType("application/json");

    jsonMapper.writeValue(
        response.getWriter(),
        new ErrorResponse(Instant.now(), 429, "Too Many Requests", List.of("Rate limit exceeded")));
  }

  private boolean shouldSkip(HttpServletRequest request) {
    String path = request.getRequestURI();
    return "OPTIONS".equals(request.getMethod())
        || path.endsWith("/health")
        || path.startsWith("/actuator")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/ws");
  }

  private RateLimitProperties.Limit resolveLimit(HttpServletRequest request) {
    String path = request.getRequestURI();

    if (path.startsWith("/auth/login")
        || path.startsWith("/auth/signup")
        || path.startsWith("/auth/refresh")) {
      return properties.getAuth();
    }

    if (path.startsWith("/transaction")) {
      return properties.getTransaction();
    }

    return properties.getDefaultLimit();
  }

  private String resolveKey(HttpServletRequest request) {
    String path = request.getRequestURI();
    String group = path.startsWith("/auth") ? "auth" : path.split("/")[1];

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      String subject = jwtAuthenticationToken.getToken().getSubject();
      return "rl:" + group + ":user:" + subject;
    }

    return "rl:" + group + ":ip:" + request.getRemoteAddr();
  }

  private record ErrorResponse(Instant timestamp, int status, String error, List<String> message) {}
}
