package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.AuthGrpcClient;
import apigateway.client.UserGrpcClient;
import apigateway.dto.request.auth.GetAuthUserByIdsRequestDto;
import apigateway.dto.request.user.GetRecipientRequestDto;
import apigateway.dto.request.user.GetRoleByAuthUserIdRequestDto;
import apigateway.dto.request.user.GetUserInfoRequestDto;
import apigateway.dto.response.user.*;
import apigateway.mapper.grpc.UserGrpcMapper;
import io.grpc.Status;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
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

    if (users.isEmpty()) {
      return List.of();
    }
    var ids = users.stream().map(GetUserInfoResponseDto::autUserId).distinct().toList();
    var authById =
        authGrpcClient.getAuthUserByIds(new GetAuthUserByIdsRequestDto(ids)).stream()
            .collect(Collectors.toMap(GetAuthUserByIdResponseDto::authUserId, value -> value));
    return users.stream()
        .map(
            user -> {
              var auth = authById.get(user.autUserId());
              if (auth == null) {
                throw Status.NOT_FOUND
                    .withDescription("Auth user not found: " + user.autUserId())
                    .asRuntimeException();
              }
              return new GetUserInfoWithAuthInfoResponseDto(
                  user, auth.role(), auth.status(), auth.socialAccounts());
            })
        .toList();
  }

  public GetRecipientInfoResponseDto getRecipientInfo(GetRecipientRequestDto request) {
    var recipient = userGrpcClient.getRecipientByEmail(request);
    var accounts = accountGrpcClient.getRecipientAccounts(recipient.userProfileId());
    return userGrpcMapper.toGetRecipientInfoResponseDto(recipient, accounts);
  }
}
