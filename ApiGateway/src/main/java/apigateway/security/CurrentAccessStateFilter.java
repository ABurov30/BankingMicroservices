package apigateway.security;

import io.grpc.StatusRuntimeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class CurrentAccessStateFilter extends OncePerRequestFilter {
  private final AccessStateRedisService accessStateRedisService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      try {
        if (!accessStateRedisService.isActive(
            UUID.fromString(jwtAuthenticationToken.getToken().getSubject()))) {
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }
      } catch (StatusRuntimeException exception) {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
