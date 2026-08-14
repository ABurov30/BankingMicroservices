package apigateway.controller;

import apigateway.client.UserGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.user.*;
import apigateway.query.UserInfoQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserGatewayController {

  private final UserGrpcClient userClient;
  private final UserInfoQueryHandler userInfoQueryHandler;
  private final CookieConfig cookieConfig;

  public UserGatewayController(
      UserGrpcClient userClient,
      CookieConfig cookieConfig,
      UserInfoQueryHandler userInfoQueryHandler) {
    this.userClient = userClient;
    this.cookieConfig = cookieConfig;
    this.userInfoQueryHandler = userInfoQueryHandler;
  }

  @GetMapping("/user-info")
  public GetUserInfoWithAuthInfoResponseDto getUserInfo(HttpServletRequest request) {
    Jwt jwt = cookieConfig.getAccessTokenJwt(request);
    UUID authUserId = UUID.fromString(jwt.getSubject());

    return userInfoQueryHandler.getUserInfoWithAuthInfo(authUserId);
  }

  @PostMapping("/user-info")
  public GetUserInfoWithAccountResponseDto getUserInfoWithAccountsByEmail(
      @Valid @RequestBody GetUserInfoByEmailRequestDto request) {
    return userInfoQueryHandler.getUserInfoWithAccountsAndCardsByEmail(request);
  }

  @GetMapping("/health")
  public String getUserHealth() {
    return userClient.getUserHealth();
  }

  @GetMapping("/manager/all-user-info")
  public List<GetUserInfoWithAuthInfoResponseDto> getAllUserInfo() {
    return userInfoQueryHandler.getAllUserInfoWithAuthInfo();
  }

  @GetMapping("/manager/user-info/{userId}")
  public GetUserInfoWithAuthInfoResponseDto getUserInfoByManager(@PathVariable UUID userId) {
    return userInfoQueryHandler.getUserInfoWithAuthInfo(userId);
  }
}
