package apigateway.mapper;

import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import enums.user.UserProfileStatus;
import org.mapstruct.Mapper;
import user.contract.v1.GetAllUserInfoGrpcResponse;
import user.contract.v1.GetUserInfoGrpcRequest;
import user.contract.v1.GetUserInfoGrpcResponse;
import user.contract.v1.UserResponse;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default GetUserInfoGrpcRequest toGetUserInfoGrpcRequest(GetUserInfoRequestDto getUserInfoRequestDto) {
        return GetUserInfoGrpcRequest.newBuilder()
                .setAuthUserId(getUserInfoRequestDto.authUserId().toString())
                .build();
    }

    default GetUserInfoResponseDto toGetInfoResponseDto(GetUserInfoGrpcResponse getUserInfoGrpcResponse) {
        return toGetInfoResponseDto(getUserInfoGrpcResponse.getUser());
    }

    default List<GetUserInfoResponseDto> toGetInfoResponseDtoList(GetAllUserInfoGrpcResponse response) {
        return response.getUsersList().stream()
                .map(this::toGetInfoResponseDto)
                .toList();
    }

    private GetUserInfoResponseDto toGetInfoResponseDto(UserResponse user) {
        return new GetUserInfoResponseDto(
                UUID.fromString(user.getUserProfileId()),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                UserProfileStatus.valueOf(user.getStatus())
        );
    }

}
