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
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInfoQueryHandler {
  private final AuthGrpcClient authGrpcClient;
  private final UserGrpcClient userGrpcClient;
  private final AccountGrpcClient accountGrpcClient;
  private final UserGrpcMapper userGrpcMapper;

  public GetUserInfoWithAuthInfoResponseDto getUserInfoWithAuthInfo(UUID authUserId) {
    CompletableFuture<GetUserInfoResponseDto> userInfoFuture =
        userGrpcClient.getUserInfoAsync(new GetUserInfoRequestDto(authUserId));

    CompletableFuture<GetAuthUserByIdResponseDto> authUserFuture =
        authGrpcClient.getAuthUserByIdAsync(new GetRoleByAuthUserIdRequestDto(authUserId));

    return userInfoFuture
        .thenCombine(
            authUserFuture,
            (userInfo, authUser) ->
                new GetUserInfoWithAuthInfoResponseDto(
                    userInfo, authUser.role(), authUser.status(), authUser.socialAccounts()))
        .join();
  }

  public List<GetUserInfoWithAuthInfoResponseDto> getAllUserInfoWithAuthInfo() {
    List<GetUserInfoResponseDto> users = userGrpcClient.getAllUserInfo();

    List<CompletableFuture<GetUserInfoWithAuthInfoResponseDto>> futures =
        users.stream()
            .map(
                user ->
                    authGrpcClient
                        .getAuthUserByIdAsync(new GetRoleByAuthUserIdRequestDto(user.autUserId()))
                        .thenApply(
                            auth ->
                                new GetUserInfoWithAuthInfoResponseDto(
                                    user, auth.role(), auth.status(), auth.socialAccounts())))
            .toList();

    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    return futures.stream().map(CompletableFuture::join).toList();
  }

  public GetRecipientInfoResponseDto getRecipientInfo(GetRecipientRequestDto request) {
    var recipient = userGrpcClient.getRecipientByEmail(request);
    var accounts = accountGrpcClient.getRecipientAccounts(recipient.userProfileId());
    return userGrpcMapper.toGetRecipientInfoResponseDto(recipient, accounts);
  }
}
