package apigateway.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import apigateway.client.*;
import apigateway.dto.request.auth.GetAuthUserByIdsRequestDto;
import apigateway.dto.response.auth.SocialAccountResponse;
import apigateway.dto.response.user.*;
import apigateway.mapper.grpc.UserGrpcMapper;
import enums.auth.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserInfoQueryHandlerTest {
  private final AuthGrpcClient auth = mock(AuthGrpcClient.class);
  private final UserGrpcClient users = mock(UserGrpcClient.class);
  private final UserInfoQueryHandler handler =
      new UserInfoQueryHandler(
          auth, users, mock(AccountGrpcClient.class), mock(UserGrpcMapper.class));

  @Test
  void joinsByIdInProfileOrderWithOneBatchCall() {
    var first = user();
    var second = user();
    var request = new GetAuthUserByIdsRequestDto(List.of(first.autUserId(), second.autUserId()));
    var social = new SocialAccountResponse(SocialLoginProvider.GOOGLE, "social@example.com");
    when(users.getAllUserInfo()).thenReturn(List.of(first, second, first));
    when(auth.getAuthUserByIds(request))
        .thenReturn(
            List.of(
                new GetAuthUserByIdResponseDto(
                    second.autUserId(), null, Roles.ADMIN, AuthUserStatus.ACTIVE, List.of(social)),
                new GetAuthUserByIdResponseDto(
                    first.autUserId(), null, Roles.USER, AuthUserStatus.BLOCKED, List.of())));
    var result = handler.getAllUserInfoWithAuthInfo();
    assertThat(result).extracting(value -> value.userInfo()).containsExactly(first, second, first);
    assertThat(result.get(0).status()).isEqualTo(AuthUserStatus.BLOCKED);
    assertThat(result.get(0).role()).isEqualTo(Roles.USER);
    assertThat(result.get(1).socialAccounts()).containsExactly(social);
    verify(auth).getAuthUserByIds(request);
    verifyNoMoreInteractions(auth);
  }

  @Test
  void emptyProfilesSkipAuthCall() {
    when(users.getAllUserInfo()).thenReturn(List.of());
    assertThat(handler.getAllUserInfoWithAuthInfo()).isEmpty();
    verifyNoInteractions(auth);
  }

  @Test
  void incompleteBatchFailsWithNotFound() {
    var user = user();
    when(users.getAllUserInfo()).thenReturn(List.of(user));
    when(auth.getAuthUserByIds(new GetAuthUserByIdsRequestDto(List.of(user.autUserId()))))
        .thenReturn(List.of());
    assertThatThrownBy(handler::getAllUserInfoWithAuthInfo)
        .isInstanceOfSatisfying(
            StatusRuntimeException.class,
            error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
  }

  private GetUserInfoResponseDto user() {
    return new GetUserInfoResponseDto(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null);
  }
}
