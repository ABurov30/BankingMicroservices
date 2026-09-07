package apigateway.mapper.command;

import apigateway.dto.command.card.GetCardsByAccountIdCommandDto;
import enums.auth.Roles;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardCommandMapper {

  default GetCardsByAccountIdCommandDto toGetCardsByAccountIdCommandDto(
      UUID accountId, UUID authUserId, Roles role) {
    return new GetCardsByAccountIdCommandDto(accountId, authUserId, role);
  }
}
