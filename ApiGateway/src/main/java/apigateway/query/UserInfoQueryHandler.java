package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.AuthGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.request.user.GetRecipientRequestDto;
import apigateway.dto.request.user.GetRoleByAuthUserIdRequestDto;
import apigateway.dto.request.user.GetUserInfoRequestDto;
import apigateway.dto.response.user.*;
import apigateway.mapper.grpc.UserGrpcMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInfoQueryHandler {
  private final AuthGrpcClient authGrpcClient;
  private final UserGrpcClient userGrpcClient;
  private final AccountGrpcClient accountGrpcClient;
  private final UserGrpcMapper userGrpcMapper;

  public GetUserInfoWithAuthInfoResponseDto getUserInfoWithAuthInfo(UUID autUserId) {
    GetUserInfoResponseDto userInfoResponseDto =
        userGrpcClient.getUserInfo(new GetUserInfoRequestDto(autUserId));
    GetAuthUserByIdResponseDto authInfo =
        authGrpcClient.getAuthUserById(new GetRoleByAuthUserIdRequestDto(autUserId));

    return new GetUserInfoWithAuthInfoResponseDto(
        userInfoResponseDto, authInfo.role(), authInfo.status(), authInfo.socialAccounts());
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
                  userInfo, authInfo.role(), authInfo.status(), authInfo.socialAccounts());
            })
        .toList();
  }

  public GetRecipientInfoResponseDto getRecipientInfo(GetRecipientRequestDto request) {
    var recipient = userGrpcClient.getRecipientByEmail(request);
    var accounts = accountGrpcClient.getRecipientAccounts(recipient.userProfileId());
    return userGrpcMapper.toGetRecipientInfoResponseDto(recipient, accounts);
  }
}
