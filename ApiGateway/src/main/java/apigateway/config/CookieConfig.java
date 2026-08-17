package apigateway.config;

import apigateway.exception.InvalidAccessTokenException;
import apigateway.exception.MissingAccessTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.StringUtils;

@Configuration
public class CookieConfig {

  private static final Logger log = LoggerFactory.getLogger(CookieConfig.class);

  private final JwtDecoder jwtDecoder;
  private final AuthCookieProperties cookieProperties;

  public CookieConfig(JwtDecoder jwtDecoder, AuthCookieProperties cookieProperties) {
    this.jwtDecoder = jwtDecoder;
    this.cookieProperties = cookieProperties;
  }

  public void setCookieTokens(
      HttpServletResponse response,
      String accessToken,
      int accessTokenMinutesTtl,
      String refreshToken,
      int refreshTokenDaysTtl) {

    ResponseCookie atCookie = createTokenCookie("at", accessToken, accessTokenMinutesTtl * 60L);

    ResponseCookie rtCookie =
        createTokenCookie("rt", refreshToken, refreshTokenDaysTtl * 24L * 60 * 60);

    response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
  }

  public void clearCookieTokens(HttpServletResponse response) {
    ResponseCookie atCookie = createTokenCookie("at", "", 0);

    ResponseCookie rtCookie = createTokenCookie("rt", "", 0);

    response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
  }

  public String getCookieByKey(HttpServletRequest request, String cookieName) {
    if (request.getCookies() == null) {
      return null;
    }

    return Arrays.stream(request.getCookies())
        .filter((c) -> cookieName.equals(c.getName()))
        .map((c) -> c.getValue())
        .findFirst()
        .orElse(null);
  }

  public Jwt getAccessTokenJwt(HttpServletRequest request) {
    String accessToken = getCookieByKey(request, "at");

    if (accessToken == null || accessToken.isBlank()) {
      throw new MissingAccessTokenException();
    }

    Jwt jwt = jwtDecoder.decode(accessToken);
    try {
      UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException exception) {
      log.warn("Access token contains an invalid subject", exception);
      throw new InvalidAccessTokenException();
    }

    return jwt;
  }

  public String extractRole(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    if (roles != null && !roles.isEmpty()) {
      return roles.get(0);
    }

    return jwt.getClaimAsString("role");
  }

  private ResponseCookie createTokenCookie(String name, String value, long maxAge) {
    ResponseCookie.ResponseCookieBuilder builder =
        ResponseCookie.from(name, value)
            .maxAge(maxAge)
            .path("/")
            .httpOnly(true)
            .secure(cookieProperties.isSecure());

    if (StringUtils.hasText(cookieProperties.getDomain())) {
      builder.domain(cookieProperties.getDomain());
    }

    if (StringUtils.hasText(cookieProperties.getSameSite())) {
      builder.sameSite(cookieProperties.getSameSite());
    }

    return builder.build();
  }
}
