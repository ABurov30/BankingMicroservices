package apigateway.controller;

import apigateway.client.UserGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
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

    public UserGatewayController(
            UserGrpcClient userClient,
            CookieConfig cookieConfig
    ) {
        this.userClient = userClient;
        this.cookieConfig = cookieConfig;
    }


    @GetMapping("/userInfo")
    public GetUserInfoResponseDto getUserInfo(HttpServletRequest request) {
        Jwt jwt = cookieConfig.getAccessTokenJwt(request);
        UUID authUserId = UUID.fromString(jwt.getSubject());
        
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
