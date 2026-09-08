package apigateway.mapper.request;

import apigateway.dto.request.user.GetUserInfoRequestDto;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserRequestMapper {
  default GetUserInfoRequestDto toGetUserInfoRequestDto(UUID authUserId) {
    return new GetUserInfoRequestDto(authUserId);
  }
}
