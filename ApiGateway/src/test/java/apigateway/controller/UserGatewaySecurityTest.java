package apigateway.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apigateway.client.AuthGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.config.SecurityConfig;
import apigateway.mapper.request.SocialLoginRequestMapper;
import apigateway.query.UserInfoQueryHandler;
import apigateway.ratelimit.RateLimitProperties;
import apigateway.ratelimit.RedisRateLimitService;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@ActiveProfiles("test")
@WebMvcTest(controllers = UserGatewayController.class, properties = "site.url=https://bank.example")
@Import(SecurityConfig.class)
public class UserGatewaySecurityTest {
  private static final String ALL_USER_INFO_URL = "/user/manager/all-user-info";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserGrpcClient userClient;
  @MockitoBean private UserInfoQueryHandler userInfoQueryHandler;
  @MockitoBean private CookieConfig cookieConfig;
  @MockitoBean private AuthGrpcClient authClient;
  @MockitoBean private SocialLoginRequestMapper socialLoginRequestMapper;

  @MockitoBean private RedisRateLimitService rateLimitService;
  @MockitoBean private RateLimitProperties rateLimitProperties;

  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

  @Test
  void shouldDenyUserAccessToAllUserInfo() throws Exception {
    mockMvc
        .perform(
            get(ALL_USER_INFO_URL)
                .with(
                    jwt()
                        .jwt(builder -> builder.claim("status", AuthUserStatus.ACTIVE.name()))
                        .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.USER.name()))))
        .andExpect(status().isForbidden());
    verify(userInfoQueryHandler, never()).getAllUserInfoWithAuthInfo();
  }

  @Test
  void shouldAllowActiveManagerAccessToAllUserInfo() throws Exception {
    mockMvc
        .perform(
            get(ALL_USER_INFO_URL)
                .with(
                    jwt()
                        .jwt(builder -> builder.claim("status", AuthUserStatus.ACTIVE.name()))
                        .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.MANAGER.name()))))
        .andExpect(status().isOk());
    when(userInfoQueryHandler.getAllUserInfoWithAuthInfo()).thenReturn(List.of());
    verify(userInfoQueryHandler, times(1)).getAllUserInfoWithAuthInfo();
  }
}
