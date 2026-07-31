package apigateway.query;

import apigateway.client.AuthGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.user.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserInfoQueryHandler {
    private final AuthGrpcClient authGrpcClient;
    private final UserGrpcClient userGrpcClient;

    public  UserInfoQueryHandler (
            AuthGrpcClient authGrpcClient,
            UserGrpcClient userGrpcClient
    ) {
        this.authGrpcClient = authGrpcClient;
        this.userGrpcClient = userGrpcClient;
    }

    public GetUserInfoWithRoleResponseDto getUserInfoWithRole (UUID autUserId) {
        GetUserInfoResponseDto userInfoResponseDto = userGrpcClient.getUserInfo(new GetUserInfoRequestDto(autUserId));
        GetRoleByAuthUserIdResponseDto getRoleByAuthUserIdResponseDto = authGrpcClient.getRoleByAuthUserId(new GetRoleByAuthUserIdRequestDto(autUserId));

        return new GetUserInfoWithRoleResponseDto(userInfoResponseDto, getRoleByAuthUserIdResponseDto);
    }

    public List<GetUserInfoWithRoleResponseDto> getAllUserInfoWithRole () {
        List<GetUserInfoResponseDto> userInfoResponseDtoList = userGrpcClient.getAllUserInfo();
        return userInfoResponseDtoList.stream()
                .map((userInfo) ->
                        new GetUserInfoWithRoleResponseDto(userInfo,
                                authGrpcClient.getRoleByAuthUserId(new GetRoleByAuthUserIdRequestDto(userInfo.autUserId()))))
                .toList();
    }
}
