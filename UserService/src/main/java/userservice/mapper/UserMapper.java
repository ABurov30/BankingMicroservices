package userservice.mapper;

import kafkacontracts.auth.AuthUserCreatedEventPayload;
import org.mapstruct.Mapper;
import user.contract.v1.GetAllUserInfoGrpcResponse;
import user.contract.v1.GetUserInfoGrpcRequest;
import user.contract.v1.GetUserInfoGrpcResponse;
import user.contract.v1.UserResponse;
import userservice.dto.CreateUserCommand;
import userservice.dto.GetUserInfoCommand;
import userservice.dto.GetUserInfoResult;
import enums.user.UserProfileStatus;
import userservice.entity.UserProfileEntity;

import java.util.List;
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

    GetUserInfoResult toGetUserInfoResult (UserProfileEntity userProfileEntity);

    CreateUserCommand toCreateUserCommand (AuthUserCreatedEventPayload authUserCreatedEventPayload);
}
