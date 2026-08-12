package apigateway.mapper.dto;

import apigateway.dto.user.GetUserInfoResponseDto;
import enums.user.UserProfileStatus;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import user.contract.v1.GetAllUserInfoGrpcResponse;
import user.contract.v1.GetUserInfoGrpcResponse;
import user.contract.v1.UserResponse;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
  default GetUserInfoResponseDto toGetInfoResponseDto(GetUserInfoGrpcResponse response) {
    return toGetInfoResponseDto(response.getUser());
  }

  private GetUserInfoResponseDto toGetInfoResponseDto(UserResponse user) {
    return new GetUserInfoResponseDto(
        UUID.fromString(user.getUserProfileId()),
        UUID.fromString(user.getAuthUserId()),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        UserProfileStatus.valueOf(user.getStatus()));
  }

  default List<GetUserInfoResponseDto> toGetInfoResponseDtoList(
      GetAllUserInfoGrpcResponse response) {
    return response.getUsersList().stream().map(this::toGetInfoResponseDto).toList();
  }
}
