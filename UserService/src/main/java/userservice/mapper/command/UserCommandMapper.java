package userservice.mapper.command;

import java.util.UUID;
import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserRoleChangedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import kafkacontracts.auth.AuthUserVerifiedEventPayload;
import org.mapstruct.Mapper;
import user.contract.v1.GetUserInfoByEmailRequest;
import user.contract.v1.GetUserInfoGrpcRequest;
import userservice.dto.*;

@Mapper(componentModel = "spring")
public interface UserCommandMapper {
  default GetUserInfoCommand toGetUserInfoCommand(GetUserInfoGrpcRequest request) {
    return new GetUserInfoCommand(UUID.fromString(request.getAuthUserId()));
  }

  default CreateUserCommand toCreateUserCommand(AuthUserCreatedEventPayload payload) {
    return new CreateUserCommand(
        payload.getAuthUserId(), payload.getEmail(), payload.getFirstName(), payload.getLastName());
  }

  default BlockedUserCommand toBlockedUserCommand(AuthUserBlockedEventPayload payload) {
    return new BlockedUserCommand(payload.getAuthUserId());
  }

  default UnlockUserCommand toUnlockUserCommand(AuthUserUnlockEventPayload payload) {
    return new UnlockUserCommand(payload.getAuthUserId());
  }

  default VerifyUserCommand toVerifyUserCommand(AuthUserVerifiedEventPayload payload) {
    return new VerifyUserCommand(payload.getAuthUserId());
  }

  default ChangeUserRoleCommand toChangeUserRoleCommand(AuthUserRoleChangedEventPayload payload) {
    return new ChangeUserRoleCommand(payload.getAuthUserId(), payload.getRole());
  }

  default GetUserInfoByEmailCommand toGetUserInfoByEmailCommand(GetUserInfoByEmailRequest request) {
    return new GetUserInfoByEmailCommand(request.getEmail());
  }
}
