package apigateway.mapper;

import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import apigateway.enums.UserProfileStatus;
import org.mapstruct.Mapper;
import user.contract.v1.GetUserInfoGrpcRequest;
import user.contract.v1.GetUserInfoGrpcResponse;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default GetUserInfoGrpcRequest toGetUserInfoGrpcRequest(GetUserInfoRequestDto getUserInfoRequestDto) {
        return GetUserInfoGrpcRequest.newBuilder()
                .setAuthUserId(getUserInfoRequestDto.authUserId().toString())
                .build();
    }

    default GetUserInfoResponseDto toGetInfoResponseDto(GetUserInfoGrpcResponse getUserInfoGrpcResponse) {
        return new GetUserInfoResponseDto(
                UUID.fromString(getUserInfoGrpcResponse.getUserProfileId()),
                getUserInfoGrpcResponse.getEmail(),
                getUserInfoGrpcResponse.getFirstName(),
                getUserInfoGrpcResponse.getLastName(),
                UserProfileStatus.valueOf(getUserInfoGrpcResponse.getStatus())
        );
    }

}
