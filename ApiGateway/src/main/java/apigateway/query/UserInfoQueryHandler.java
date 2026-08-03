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

    public GetUserInfoWithAuthInfoResponseDto getUserInfoWithAuthInfo (UUID autUserId) {
        GetUserInfoResponseDto userInfoResponseDto = userGrpcClient.getUserInfo(new GetUserInfoRequestDto(autUserId));
        GetAuthUserByIdResponseDto authInfo = authGrpcClient.getAuthUserById(new GetRoleByAuthUserIdRequestDto(autUserId));

        return new GetUserInfoWithAuthInfoResponseDto(userInfoResponseDto, authInfo.role(), authInfo.status());
    }

    public List<GetUserInfoWithAuthInfoResponseDto> getAllUserInfoWithAuthInfo () {
        List<GetUserInfoResponseDto> userInfoResponseDtoList = userGrpcClient.getAllUserInfo();
        return userInfoResponseDtoList.stream()
                .map((userInfo) -> {
                    GetAuthUserByIdResponseDto authInfo = authGrpcClient.getAuthUserById(new GetRoleByAuthUserIdRequestDto(userInfo.autUserId()));
                    return new GetUserInfoWithAuthInfoResponseDto(userInfo, authInfo.role(), authInfo.status());
                })
                .toList();
    }
}
