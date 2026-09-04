package apigateway.controller;

import apigateway.client.AuthGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.auth.*;
import apigateway.exception.MissingRefreshTokenException;
import enums.auth.Roles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthGatewayController {

  private final AuthGrpcClient authClient;
  private final CookieConfig cookieConfig;

  public AuthGatewayController(AuthGrpcClient authClient, CookieConfig cookieConfig) {
    this.cookieConfig = cookieConfig;
    this.authClient = authClient;
  }

  @ResponseStatus(HttpStatus.OK)
  @PostMapping("/signup")
  public void signup(@Valid @RequestBody SignupRequestDto request) {
    authClient.signup(request);
  }

  @PostMapping("/login")
  public void login(@Valid @RequestBody LoginRequestDto request, HttpServletResponse response) {
    LoginResponseDto loginResponse = authClient.login(request);

    cookieConfig.setCookieTokens(
        response,
        loginResponse.accessToken(),
        (int) loginResponse.accessTokenMinutesTtl(),
        loginResponse.refreshToken(),
        (int) loginResponse.refreshTokenDaysTtl());
  }

  @DeleteMapping("/logout")
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = cookieConfig.getCookieByKey(request, "rt");

    if (refreshToken == null || refreshToken.isBlank()) {
      throw new MissingRefreshTokenException();
    }

    authClient.logout(new LogoutRequestDto(refreshToken));
    cookieConfig.clearCookieTokens(response);
  }

  @PostMapping("/refresh")
  public void refresh(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = cookieConfig.getCookieByKey(request, "rt");

    if (refreshToken == null || refreshToken.isBlank()) {
      throw new MissingRefreshTokenException();
    }

    RefreshResponseDto refreshResponse = authClient.refresh(new RefreshRequestDto(refreshToken));

    cookieConfig.setCookieTokens(
        response,
        refreshResponse.accessToken(),
        (int) refreshResponse.accessTokenMinutesTtl(),
        refreshResponse.refreshToken(),
        (int) refreshResponse.refreshTokenDaysTtl());
  }

  @PutMapping("/change-password")
  public void changePassword(
      @Valid @RequestBody ChangePasswordRequestDto request,
      HttpServletRequest httpRequest,
      HttpServletResponse servletResponse) {
    Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);

    ChangePasswordResponseDto response =
        authClient.changePassword(request, UUID.fromString(jwt.getSubject()));

    cookieConfig.setRefreshTokenCookie(
        servletResponse, response.refreshToken(), Math.toIntExact(response.refreshTokenDaysTtl()));
  }

  @PutMapping("/verify-user")
  public void verifyUser(
      @Valid @RequestBody VerifyAuthUserByCodeRequestDto request, HttpServletResponse response) {
    VerifyAuthUserByCodeResponseDto verifyResponse = authClient.verifyByCode(request);

    cookieConfig.setCookieTokens(
        response,
        verifyResponse.accessToken(),
        (int) verifyResponse.accessTokenMinutesTtl(),
        verifyResponse.refreshToken(),
        (int) verifyResponse.refreshTokenDaysTtl());
  }

  @PostMapping("/forget-password")
  public void forgetPassword(@Valid @RequestBody ForgetPasswordRequestDto request) {
    authClient.forgetPassword(request);
  }

  @PutMapping("/reset-password")
  public void resetPassword(
      @Valid @RequestBody ResetPasswordRequestDto request, HttpServletResponse response) {
    ResetPasswordsResponseDto resetPasswordResponse = authClient.resetPassword(request);

    cookieConfig.setCookieTokens(
        response,
        resetPasswordResponse.accessToken(),
        (int) resetPasswordResponse.accessTokenMinutesTtl(),
        resetPasswordResponse.refreshToken(),
        (int) resetPasswordResponse.refreshTokenDaysTtl());
  }

  @GetMapping("/health")
  public String getAuthHealth() {
    return authClient.getAuthHealth();
  }

  @PutMapping("/manager/block-user")
  public void blockUserByManager(@Valid @RequestBody BlockAuthUserRequestDto request) {
    authClient.blockUser(request);
  }

  @PutMapping("/manager/unlock-user")
  public void unlockUserByManager(@Valid @RequestBody UnlockAuthUserRequestDto request) {
    authClient.unlockUser(request);
  }

  @PutMapping("/manager/verify-user/{authUserId}")
  public void verifyUserByManager(@PathVariable UUID authUserId, HttpServletRequest httpRequest) {
    Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);
    VerifyAuthUserByPrivilegeRoleRequestDto request =
        new VerifyAuthUserByPrivilegeRoleRequestDto(
            authUserId, Roles.valueOf(cookieConfig.extractRole(jwt)));

    authClient.verifyByPrivilegedRole(request);
  }

  @PutMapping("/admin/change-auth-user-role")
  public void changeAuthUserRole(@Valid @RequestBody ChangeAuthUserRoleRequestDto request) {
    authClient.changeAuthUserRole(request);
  }

  @GetMapping("/oauth/google")
  public void googleLogin(HttpServletResponse response) throws IOException {
    response.sendRedirect("/oauth2/authorization/google");
  }
}
