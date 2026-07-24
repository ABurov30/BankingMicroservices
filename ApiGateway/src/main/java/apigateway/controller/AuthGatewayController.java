package apigateway.controller;

import apigateway.client.AuthGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.auth.*;
import apigateway.exception.InvalidAccessTokenException;
import apigateway.exception.MissingAccessTokenException;
import apigateway.exception.MissingRefreshTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthGatewayController {

    private final AuthGrpcClient authClient;
    private final CookieConfig cookieConfig;
    private final JwtDecoder jwtDecoder;


    public AuthGatewayController(
            AuthGrpcClient authClient,
            CookieConfig cookieConfig,
            JwtDecoder jwtDecoder
    ) {
        this.cookieConfig = cookieConfig;
        this.authClient = authClient;
        this.jwtDecoder = jwtDecoder;
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

    @PutMapping("/change-password")
    public void ChangePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
        authClient.changePassword(request);
    }

    @PutMapping("/verify-user")
    public void VerifyUserByManager(@Valid @RequestBody VerifyUserRequestDto request, HttpServletRequest httpRequest){
        Jwt jwt = getAccessTokenJwt(httpRequest);
        VerifyUserRequestDto verifyUserRequestDto = new VerifyUserRequestDto(
                UUID.fromString(jwt.getSubject()),
                request.verificationCode(),
                null
        );
        authClient.verifyUser(verifyUserRequestDto);
    }


    @GetMapping("/health")
    public String getAuthHealth() {
        return authClient.getAuthHealth();
    }

    @PutMapping("/manager/block-user")
    public void BlockUserByManager(@Valid @RequestBody BlockAuthUserRequestDto request) {
        authClient.blockUser(request);
    }

    @PutMapping("/manager/unlock-user")
    public void UnlockUserByManager(@Valid @RequestBody UnlockAuthUserRequestDto request) {
        authClient.unlockUser(request);
    }

    @PutMapping("/manager/verify-user/{authUserId}")
    public void VerifyUserByManager(@PathVariable UUID authUserId, HttpServletRequest httpRequest){
        Jwt jwt = getAccessTokenJwt(httpRequest);
        VerifyUserRequestDto verifyUserRequestDto = new VerifyUserRequestDto(
                authUserId,
                null,
                extractRole(jwt)
        );
        authClient.verifyUser(verifyUserRequestDto);
    }

    private Jwt getAccessTokenJwt(HttpServletRequest request) {
        String accessToken = cookieConfig.getCookieByKey(request, "at");

        if (accessToken == null || accessToken.isBlank()) {
            throw new MissingAccessTokenException();
        }

        Jwt jwt = jwtDecoder.decode(accessToken);
        try {
            UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }

        return jwt;
    }

    private String extractRole(Jwt jwt) {
        java.util.List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }

        return jwt.getClaimAsString("role");
    }
}
