package apigateway.mapper.command;

import apigateway.dto.command.account.*;
import apigateway.dto.request.account.CreateAccountRequestDto;
import enums.auth.Roles;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountCommandMapper {
  default GetAccountsWithCardsByOwnerIdCommandDto toGetAccountsWithCardsByOwnerIdCommandDto(
      UUID ownerUserId, UUID authUserId, Roles role) {
    return new GetAccountsWithCardsByOwnerIdCommandDto(ownerUserId, authUserId, role);
  }

  default CheckAccountStatusCommandDto toCheckAccountStatusCommandDto(
      UUID accountId, UUID authUserId, Roles role) {
    return new CheckAccountStatusCommandDto(accountId, authUserId, role);
  }

  default GetAllAccountsWithCardsCommandDto toGetAllAccountsWithCardsCommandDto(
      UUID authUserId, Roles role) {
    return new GetAllAccountsWithCardsCommandDto(authUserId, role);
  }

  default GetAllAccountsWithCardsByAuthUserIdCommandDto
      toGetAllAccountsWithCardsByAuthUserIdCommandDto(UUID authUserId, Roles role) {
    return new GetAllAccountsWithCardsByAuthUserIdCommandDto(authUserId, role);
  }

  default CreateAccountCommandDto toCreateAccountCommandDto(
      CreateAccountRequestDto requestDto, UUID authUserId) {
    return new CreateAccountCommandDto(requestDto.type(), requestDto.currency(), authUserId);
  }
}
