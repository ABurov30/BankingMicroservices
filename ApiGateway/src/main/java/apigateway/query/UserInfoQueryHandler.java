package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.AuthGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.user.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserInfoQueryHandler {
  private final AuthGrpcClient authGrpcClient;
  private final UserGrpcClient userGrpcClient;
  private final AccountGrpcClient accountGrpcClient;

  public UserInfoQueryHandler(
      AuthGrpcClient authGrpcClient,
      UserGrpcClient userGrpcClient,
      AccountGrpcClient accountGrpcClient) {
    this.authGrpcClient = authGrpcClient;
    this.userGrpcClient = userGrpcClient;
    this.accountGrpcClient = accountGrpcClient;
  }

  public GetUserInfoWithAuthInfoResponseDto getUserInfoWithAuthInfo(UUID autUserId) {
    GetUserInfoResponseDto userInfoResponseDto =
        userGrpcClient.getUserInfo(new GetUserInfoRequestDto(autUserId));
    GetAuthUserByIdResponseDto authInfo =
        authGrpcClient.getAuthUserById(new GetRoleByAuthUserIdRequestDto(autUserId));

    return new GetUserInfoWithAuthInfoResponseDto(
        userInfoResponseDto, authInfo.role(), authInfo.status());
  }

  public List<GetUserInfoWithAuthInfoResponseDto> getAllUserInfoWithAuthInfo() {
    List<GetUserInfoResponseDto> userInfoResponseDtoList = userGrpcClient.getAllUserInfo();
    return userInfoResponseDtoList.stream()
        .map(
            (userInfo) -> {
              GetAuthUserByIdResponseDto authInfo =
                  authGrpcClient.getAuthUserById(
                      new GetRoleByAuthUserIdRequestDto(userInfo.autUserId()));
              return new GetUserInfoWithAuthInfoResponseDto(
                  userInfo, authInfo.role(), authInfo.status());
            })
        .toList();
  }

  public GetUserInfoWithAccountResponseDto getUserInfoWithAccountByEmail(
      GetUserInfoByEmailRequestDto request) {
    var userInfo = userGrpcClient.getUserInfoByEmail(request);
    var accounts = accountGrpcClient.getAccountsByOwnerId(userInfo.userProfileId());
    return new GetUserInfoWithAccountResponseDto(userInfo, accounts);
  }
}
