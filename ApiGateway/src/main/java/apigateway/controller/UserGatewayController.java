package apigateway.controller;

import apigateway.client.UserGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.config.JwtConfig;
import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import apigateway.exception.InvalidAccessTokenException;
import apigateway.exception.MissingAccessTokenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserGatewayController {

    private final UserGrpcClient userClient;
    private final CookieConfig cookieConfig;
    private final JwtDecoder jwtDecoder;

    public UserGatewayController(
            UserGrpcClient userClient,
            CookieConfig cookieConfig,
            JwtDecoder jwtDecoder
    ) {
        this.userClient = userClient;
        this.cookieConfig = cookieConfig;
        this.jwtDecoder = jwtDecoder;
    }


    @GetMapping("/userInfo")
    public GetUserInfoResponseDto getUserInfo(HttpServletRequest request) {
        String accessToken = cookieConfig.getCookieByKey(request, "at");

        if (accessToken == null || accessToken.isBlank()) {
            throw new MissingAccessTokenException();
        }

        Jwt jwt = jwtDecoder.decode(accessToken);
        UUID authUserId;
        try {
            authUserId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }
        
        return userClient.getUserInfo(new GetUserInfoRequestDto(authUserId));
    }

    @GetMapping("/health")
    public String getUserHealth() {
        return userClient.getUserHealth();
    }

    @GetMapping("/manager/all-userinfo")
    public List<GetUserInfoResponseDto> getAllUserInfo() {
        return userClient.getAllUserInfo();
    }
}
