package userservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import enums.user.UserProfileStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import org.junit.jupiter.api.Test;
import user.contract.v1.GetAllUserInfoGrpcResponse;
import user.contract.v1.GetRecipientByEmailRequest;
import user.contract.v1.UserResponse;
import userservice.dto.GetUserInfoResult;
import userservice.entity.UserProfileEntity;
import userservice.mapper.command.UserCommandMapperImpl;
import userservice.mapper.eventpayload.UserEventPayloadMapperImpl;
import userservice.mapper.grpc.UserGrpcMapperImpl;
import userservice.mapper.result.UserResultMapperImpl;

class UserMappersTest {
  private final UUID userId = UUID.randomUUID();
  private final UUID profileId = UUID.randomUUID();

  @Test
  void mapsAuthPayloadAndGrpcRequests() {
    var auth =
        AuthUserCreatedEventPayload.newBuilder()
            .setAuthUserId(userId)
            .setEmail("user@test")
            .setFirstName("First")
            .setLastName("Last")
            .setStatus(UserProfileStatus.ACTIVE.name())
            .setRole("USER")
            .setVerificationCode("code")
            .build();
    var mapper = new UserCommandMapperImpl();
    assertThat(mapper.toCreateUserCommand(auth).authUserId()).isEqualTo(userId);
    assertThat(mapper.toCreateUserCommand(auth).status()).isEqualTo(UserProfileStatus.ACTIVE);
    assertThat(
            mapper
                .toGetUserInfoByEmailCommand(
                    GetRecipientByEmailRequest.newBuilder().setEmail("user@test").build())
                .email())
        .isEqualTo("user@test");
  }

  @Test
  void mapsProfileEventsAndGrpcResponses() {
    var eventMapper = new UserEventPayloadMapperImpl();
    var event =
        eventMapper.toUserProfileCreatedEventPayload(
            Map.of("userId", profileId.toString(), "authUserId", userId.toString()));
    assertThat(event.getUserId()).isEqualTo(profileId);
    assertThat(event.getAuthUserId()).isEqualTo(userId);
    var result =
        new GetUserInfoResult(
            profileId, userId, "user@test", "First", "Last", UserProfileStatus.ACTIVE);
    var grpc = new UserGrpcMapperImpl();
    UserResponse response = grpc.toUserResponse(result);
    GetAllUserInfoGrpcResponse all = grpc.toGetAllUserInfoGrpcResponse(List.of(response));
    assertThat(all.getUsersCount()).isEqualTo(1);
    assertThat(grpc.toGetRecipientResponse(result).getUser().getFirstName()).isEqualTo("First");
    var entity = new UserProfileEntity();
    entity.setId(profileId);
    entity.setAuthUserId(userId);
    entity.setEmail("user@test");
    entity.setFirstName("First");
    entity.setLastName("Last");
    entity.setStatus(UserProfileStatus.ACTIVE);
    assertThat(new UserResultMapperImpl().toGetUserInfoResult(entity).authUserId())
        .isEqualTo(userId);
  }
}
