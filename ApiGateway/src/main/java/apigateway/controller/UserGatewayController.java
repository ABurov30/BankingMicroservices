package apigateway.controller;

import apigateway.client.UserGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import apigateway.dto.user.GetUserInfoWithAuthInfoResponseDto;
import apigateway.query.UserInfoQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserGatewayController {

    private final UserGrpcClient userClient;
    private final UserInfoQueryHandler userInfoQueryHandler;
    private final CookieConfig cookieConfig;

    public UserGatewayController(
            UserGrpcClient userClient,
            CookieConfig cookieConfig,
            UserInfoQueryHandler userInfoQueryHandler
    ) {
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
