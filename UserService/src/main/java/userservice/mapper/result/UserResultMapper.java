package userservice.mapper.result;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import userservice.dto.GetUserInfoResult;
import userservice.entity.UserProfileEntity;

@Mapper(componentModel = "spring")
public interface UserResultMapper {
  @Mapping(target = "userProfileId", source = "id")
  GetUserInfoResult toGetUserInfoResult(UserProfileEntity userProfileEntity);
}
