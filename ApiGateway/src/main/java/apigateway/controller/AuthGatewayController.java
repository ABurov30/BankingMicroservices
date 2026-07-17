package apigateway.controller;

import apigateway.client.AuthGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.auth.*;
import apigateway.exception.MissingRefreshTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public void Signup(@Valid @RequestBody SignupRequestDto request, HttpServletResponse response) {
        SignupResponseDto signupResponse = authClient.signup(request);

        cookieConfig.setCookieTokens(
                response,
                signupResponse.accessToken(),
                (int) signupResponse.accessTokenMinutesTtl(),
                signupResponse.refreshToken(),
                (int) signupResponse.refreshTokenDaysTtl());
    }

    ;

    @PostMapping("/login")
    public void Login(@Valid @RequestBody LoginRequestDto request, HttpServletResponse response) {
        LoginResponseDto loginResponse = authClient.login(request);

        cookieConfig.setCookieTokens(
                response,
                loginResponse.accessToken(),
                (int) loginResponse.accessTokenMinutesTtl(),
                loginResponse.refreshToken(),
                (int) loginResponse.refreshTokenDaysTtl());
    }

    ;

    @DeleteMapping("/logout")
    public void Logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieConfig.getCookieByKey(request, "rt");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new MissingRefreshTokenException();
        }

        authClient.logout(new LogoutRequestDto(refreshToken));
        cookieConfig.clearCookieTokens(response);
    }

    @PostMapping("/refresh")
    public void Refresh(HttpServletRequest request, HttpServletResponse response) {
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

    @GetMapping("/health")
    public String getAuthHealth() {
        return authClient.getAuthHealth();
    }
}
