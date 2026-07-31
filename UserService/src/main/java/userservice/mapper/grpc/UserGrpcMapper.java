package userservice.mapper.grpc;

import org.mapstruct.Mapper;
import user.contract.v1.GetAllUserInfoGrpcResponse;
import user.contract.v1.GetUserInfoGrpcResponse;
import user.contract.v1.UserResponse;
import userservice.dto.GetUserInfoResult;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserGrpcMapper {
    default GetUserInfoGrpcResponse toGetUserInfoGrpcResponse(GetUserInfoResult result) {
        return GetUserInfoGrpcResponse.newBuilder().setUser(toUserResponse(result)).build();
    }

    default UserResponse toUserResponse(GetUserInfoResult result) {
        return UserResponse.newBuilder()
                .setUserProfileId(result.userProfileId().toString())
                .setAuthUserId(result.authUserId().toString())
                .setEmail(result.email())
                .setFirstName(result.firstName())
                .setLastName(result.lastName())
                .setStatus(result.status().toString())
                .build();
    }

    default GetAllUserInfoGrpcResponse toGetAllUserInfoGrpcResponse(List<UserResponse> users) {
        return GetAllUserInfoGrpcResponse.newBuilder().addAllUsers(users).build();
    }
}
