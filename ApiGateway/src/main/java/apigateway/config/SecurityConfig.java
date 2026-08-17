package apigateway.config;

import apigateway.client.AuthGrpcClient;
import apigateway.dto.auth.LoginResponseDto;
import apigateway.dto.auth.SocialLoginRequestDto;
import apigateway.mapper.dto.SocialLoginDtoMapper;
import apigateway.ratelimit.RateLimitFilter;
import apigateway.ratelimit.RateLimitProperties;
import apigateway.ratelimit.RedisRateLimitService;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {
  private static final String[] PUBLIC_ENDPOINTS = {
    "/auth/signup",
    "/auth/login",
    "/auth/refresh",
    "/auth/oauth/google",
    "/auth/logout",
    "/auth/verify-user",
    "/oauth2/**",
    "/login/oauth2/**",
    "/ws",
    "/ws/**",
    "/*/health",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/actuator/health",
    "/actuator/prometheus"
  };

  private final CookieConfig cookieConfig;
  private final AuthGrpcClient authClient;
  private final SocialLoginDtoMapper socialLoginDtoMapper;

  public SecurityConfig(
      CookieConfig cookieConfig,
      AuthGrpcClient authClient,
      SocialLoginDtoMapper socialLoginDtoMapper) {
    this.cookieConfig = cookieConfig;
    this.authClient = authClient;
    this.socialLoginDtoMapper = socialLoginDtoMapper;
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
    FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  RateLimitFilter rateLimitFilter(
      RedisRateLimitService service, RateLimitProperties properties, JsonMapper jsonMapper) {
    return new RateLimitFilter(service, properties, jsonMapper);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${site.url}") String siteUrl) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(toOrigin(siteUrl)));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  AuthenticationSuccessHandler oauth2SuccessHandler(@Value("${site.url}") String siteUrl) {
    return (request, response, authentication) -> {
      OidcUser user = (OidcUser) authentication.getPrincipal();
      SocialLoginRequestDto socialLoginRequest = socialLoginDtoMapper.toSocialLoginRequestDto(user);
      LoginResponseDto loginResponse = authClient.socialLogin(socialLoginRequest);

      cookieConfig.setCookieTokens(
          response,
          loginResponse.accessToken(),
          (int) loginResponse.accessTokenMinutesTtl(),
          loginResponse.refreshToken(),
          (int) loginResponse.refreshTokenDaysTtl());

      response.sendRedirect(siteUrl);
    };
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity httpSecurity,
      RateLimitFilter rateLimitFilter,
      CorsConfigurationSource corsConfigurationSource,
      AuthenticationSuccessHandler oauth2SuccessHandler)
      throws Exception {
    return httpSecurity
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .oauth2Login(oauth -> oauth.successHandler(oauth2SuccessHandler))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/auth/admin/**")
                    .access(
                        (authentication, context) ->
                            new AuthorizationDecision(
                                hasRole(authentication.get(), Roles.ADMIN)
                                    && isActive(authentication.get())))
                    .requestMatchers(PUBLIC_ENDPOINTS)
                    .permitAll()
                    .requestMatchers("/auth/manager/**", "/user/manager/**", "/account/manager/**")
                    .access(
                        (authentication, context) ->
                            new AuthorizationDecision(
                                hasRole(authentication.get(), Roles.ADMIN)
                                    || (hasRole(authentication.get(), Roles.MANAGER)
                                        && isActive(authentication.get()))))
                    .anyRequest()
                    .access(
                        (authentication, context) -> {
                          return new AuthorizationDecision(
                              isActive(authentication.get())
                                  || hasRole(authentication.get(), Roles.ADMIN));
                        }))
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .bearerTokenResolver(
                        request -> {
                          String path = request.getRequestURI();
                          if (isPublicEndpoint(path)) {
                            return null;
                          }
                          return cookieConfig.getCookieByKey(request, "at");
                        })
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)
        .build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    var jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return jwtConverter;
  }

  private boolean isActive(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
      return false;
    }

    Jwt jwt = jwtAuthenticationToken.getToken();
    String status = jwt.getClaimAsString("status");

    return AuthUserStatus.ACTIVE.name().equals(status);
  }

  private boolean hasRole(Authentication authentication, Roles role) {
    String roleAuthority = "ROLE_" + role.name();

    return authentication.getAuthorities().stream()
        .anyMatch(authority -> roleAuthority.equals(authority.getAuthority()));
  }

  private boolean isPublicEndpoint(String path) {
    return path.equals("/auth/signup")
        || path.equals("/auth/login")
        || path.equals("/auth/refresh")
        || path.equals("/auth/logout")
        || path.equals("/auth/verify-user")
        || path.startsWith("/oauth2/")
        || path.equals("/auth/oauth/google")
        || path.startsWith("/login/oauth2/")
        || path.equals("/ws")
        || path.startsWith("/ws/")
        || path.endsWith("/health")
        || path.startsWith("/v3/api-docs/")
        || path.equals("/swagger-ui.html")
        || path.startsWith("/swagger-ui/")
        || path.equals("/actuator/health")
        || path.equals("/actuator/prometheus");
  }

  private String toOrigin(String siteUrl) {
    URI siteUri = URI.create(siteUrl);
    return siteUri.getScheme() + "://" + siteUri.getAuthority();
  }
}
