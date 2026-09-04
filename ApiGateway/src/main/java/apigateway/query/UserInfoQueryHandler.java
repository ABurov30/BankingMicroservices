package apigateway.query;

import apigateway.client.AuthGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.account.GetAccountWithCardsResponseDto;
import apigateway.dto.user.*;
import apigateway.mapper.grpc.UserGrpcMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserInfoQueryHandler {
  private final AuthGrpcClient authGrpcClient;
  private final UserGrpcClient userGrpcClient;
  private final AccountQueryHandler accountQueryHandler;
  private final UserGrpcMapper userGrpcMapper;

  public UserInfoQueryHandler(
      AuthGrpcClient authGrpcClient,
      UserGrpcClient userGrpcClient,
      AccountQueryHandler accountQueryHandler,
      UserGrpcMapper userGrpcMapper) {
    this.authGrpcClient = authGrpcClient;
    this.userGrpcClient = userGrpcClient;
    this.accountQueryHandler = accountQueryHandler;
    this.userGrpcMapper = userGrpcMapper;
  }

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
    var userInfo = userGrpcClient.getUserInfoByEmail(request);
    var accounts = accountQueryHandler.getAccountsWithCardsByOwnerId(userInfo.userProfileId());
    return userGrpcMapper.toGetRecipientInfoResponseDto(userInfo, accounts);
  }

  private GetUserInfoAccountWithCardsResponseDto toUserInfoAccountWithCardsResponseDto(
      GetAccountWithCardsResponseDto accountWithCards) {
    return new GetUserInfoAccountWithCardsResponseDto(
        toUserInfoAccountResponseDto(accountWithCards.account()), accountWithCards.cards());
  }

  private GetUserInfoAccountResponseDto toUserInfoAccountResponseDto(
      GetAccountResponseDto account) {
    return new GetUserInfoAccountResponseDto(
        account.accountId(),
        account.ownerUserId(),
        account.accountNumber(),
        account.type(),
        account.status(),
        account.currency());
  }
}
