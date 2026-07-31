package apigateway.mapper.dto;

import apigateway.dto.user.GetUserInfoResponseDto;
import enums.user.UserProfileStatus;
import org.mapstruct.Mapper;
import user.contract.v1.GetAllUserInfoGrpcResponse;
import user.contract.v1.GetUserInfoGrpcResponse;
import user.contract.v1.UserResponse;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    default GetUserInfoResponseDto toGetInfoResponseDto(GetUserInfoGrpcResponse response) { return toGetInfoResponseDto(response.getUser()); }
    default List<GetUserInfoResponseDto> toGetInfoResponseDtoList(GetAllUserInfoGrpcResponse response) { return response.getUsersList().stream().map(this::toGetInfoResponseDto).toList(); }
    private GetUserInfoResponseDto toGetInfoResponseDto(UserResponse user) { return new GetUserInfoResponseDto(UUID.fromString(user.getUserProfileId()), UUID.fromString(user.getAuthUserId()), user.getEmail(), user.getFirstName(), user.getLastName(), UserProfileStatus.valueOf(user.getStatus())); }
}
