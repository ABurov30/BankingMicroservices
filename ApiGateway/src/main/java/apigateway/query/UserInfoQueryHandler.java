package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.AuthGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.account.GetAccountWithCardsResponseDto;
import apigateway.dto.user.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserInfoQueryHandler {
  private final AuthGrpcClient authGrpcClient;
  private final UserGrpcClient userGrpcClient;
  private final AccountGrpcClient accountGrpcClient;
  private final AccountQueryHandler accountQueryHandler;

  public UserInfoQueryHandler(
      AuthGrpcClient authGrpcClient,
      UserGrpcClient userGrpcClient,
      AccountGrpcClient accountGrpcClient,
      AccountQueryHandler accountQueryHandler) {
    this.authGrpcClient = authGrpcClient;
    this.userGrpcClient = userGrpcClient;
    this.accountGrpcClient = accountGrpcClient;
    this.accountQueryHandler = accountQueryHandler;
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

  public GetUserInfoWithAccountResponseDto getUserInfoWithAccountsAndCardsByEmail(
      GetUserInfoByEmailRequestDto request) {
    var userInfo = userGrpcClient.getUserInfoByEmail(request);
    var accounts = accountQueryHandler.getAccountsWithCardsByOwnerId(userInfo.userProfileId());
    return new GetUserInfoWithAccountResponseDto(
        userInfo, accounts.stream().map(this::toUserInfoAccountWithCardsResponseDto).toList());
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
