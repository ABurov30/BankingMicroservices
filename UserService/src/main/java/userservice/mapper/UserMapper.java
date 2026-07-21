package userservice.mapper;

import kafkacontracts.auth.AuthUserCreatedEventPayload;
import org.mapstruct.Mapper;
import user.contract.v1.GetUserInfoGrpcRequest;
import user.contract.v1.GetUserInfoGrpcResponse;
import userservice.dto.CreateUserCommand;
import userservice.dto.GetUserInfoCommand;
import userservice.dto.GetUserInfoResult;
import enums.user.UserProfileStatus;

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
                .setUserProfileId(getUserInfoResult.userProfileId().toString())
                .setEmail(getUserInfoResult.email())
                .setFirstName(getUserInfoResult.firstName())
                .setLastName(getUserInfoResult.lastName())
                .setStatus(getUserInfoResult.status().toString())
                .build();
    }

    CreateUserCommand toCreateUserCommand (AuthUserCreatedEventPayload authUserCreatedEventPayload);
}
