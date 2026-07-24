package userservice.mapper;

import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import kafkacontracts.user.UserProfileBlockedEventPayload;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import kafkacontracts.user.UserProfileUnlockEventPayload;
import org.mapstruct.Mapper;
import user.contract.v1.GetAllUserInfoGrpcResponse;
import user.contract.v1.GetUserInfoGrpcRequest;
import user.contract.v1.GetUserInfoGrpcResponse;
import user.contract.v1.UserResponse;
import userservice.dto.*;
import enums.user.UserProfileStatus;
import userservice.entity.UserProfileEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default GetUserInfoCommand toGetUserInfoCommand(GetUserInfoGrpcRequest getUserInfoGrpcRequest) {
        return new GetUserInfoCommand(
                UUID.fromString(getUserInfoGrpcRequest.getAuthUserId())
        );
    }

    default GetUserInfoGrpcResponse toGetUserInfoGrpcResponse(GetUserInfoResult getUserInfoResult) {
        return GetUserInfoGrpcResponse.newBuilder()
                .setUser(toUserResponse(getUserInfoResult))
                .build();
    }

    default UserResponse toUserResponse(GetUserInfoResult getUserInfoResult) {
        return UserResponse.newBuilder()
                .setUserProfileId(getUserInfoResult.userProfileId().toString())
                .setEmail(getUserInfoResult.email())
                .setFirstName(getUserInfoResult.firstName())
                .setLastName(getUserInfoResult.lastName())
                .setStatus(getUserInfoResult.status().toString())
                .build();
    }

    default GetAllUserInfoGrpcResponse toGetAllUserInfoGrpcResponse(List<UserResponse> userResponses) {
        return GetAllUserInfoGrpcResponse.newBuilder()
                .addAllUsers(userResponses)
                .build();
    }

    GetUserInfoResult toGetUserInfoResult(UserProfileEntity userProfileEntity);

    default CreateUserCommand toCreateUserCommand(AuthUserCreatedEventPayload payload) {
        return new CreateUserCommand(
                payload.getAuthUserId(),
                payload.getEmail(),
                payload.getFirstName(),
                payload.getLastName()
        );
    }

    default BlockedUserCommand toBlockedUserCommand(AuthUserBlockedEventPayload payload) {
        return new BlockedUserCommand(
                payload.getAuthUserId()
        );
    }

    default UnlockUserCommand toUnlockUserCommand(AuthUserUnlockEventPayload payload) {
        return new UnlockUserCommand(
                payload.getAuthUserId()
        );
    }

    default UserProfileCreatedEventPayload toUserProfileCreatedEventPayload(Map<String, Object> payload) {
        return UserProfileCreatedEventPayload.newBuilder()
                .setUserId(UUID.fromString(payload.get("userId").toString()))
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .build();
    }

    default UserProfileBlockedEventPayload toUserProfileBlockedEventPayload(Map<String, Object> payload) {
        return UserProfileBlockedEventPayload.newBuilder()
                .setUserId(UUID.fromString(payload.get("userId").toString()))
                .build();
    }

    default UserProfileUnlockEventPayload toUserProfileUnlockEventPayload(Map<String, Object> payload) {
        return UserProfileUnlockEventPayload.newBuilder()
                .setUserId(UUID.fromString(payload.get("userId").toString()))
                .build();
    }
}
