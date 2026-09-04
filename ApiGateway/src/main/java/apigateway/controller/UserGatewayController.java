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

  @PostMapping("/recipient-info")
  public GetRecipientInfoResponseDto getRecipientInfo(
      @Valid @RequestBody GetRecipientRequestDto request) {
    return userInfoQueryHandler.getRecipientInfo(request);
  }

  @GetMapping("/health")
  public String getUserHealth() {
    return userClient.getUserHealth();
  }

  @GetMapping("/manager/all-user-info")
  public List<GetUserInfoWithAuthInfoResponseDto> getAllUserInfo() {
    return userInfoQueryHandler.getAllUserInfoWithAuthInfo();
  }

  @PostMapping("/manager/user-info")
  public GetUserInfoWithAuthInfoResponseDto getUserInfoByManager(
      @Valid @RequestBody GetUserInfoByManagerRequestDto request) {
    return userInfoQueryHandler.getUserInfoWithAuthInfo(request.userId());
  }
}
