package apigateway.mapper.command;

import apigateway.dto.command.account.CheckAccountStatusCommandDto;
import apigateway.dto.command.account.GetAccountsWithCardsByOwnerIdCommandDto;
import apigateway.dto.command.account.GetAllAccountsWithCardsCommandDto;
import apigateway.dto.command.transaction.GetTransactionByUserIdCommandDto;
import enums.auth.Roles;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountCommandMapper {
  default GetAccountsWithCardsByOwnerIdCommandDto toGetAccountsWithCardsByOwnerIdCommandDto(
      UUID ownerUserId, UUID authUserId, Roles role) {
    return new GetAccountsWithCardsByOwnerIdCommandDto(ownerUserId, authUserId, role);
  }

  GetAccountsWithCardsByOwnerIdCommandDto toGetAccountsWithCardsByOwnerIdCommandDto(
      GetTransactionByUserIdCommandDto command);

  default CheckAccountStatusCommandDto toCheckAccountStatusCommandDto(
      UUID accountId, UUID authUserId, Roles role) {
    return new CheckAccountStatusCommandDto(accountId, authUserId, role);
  }

  default GetAllAccountsWithCardsCommandDto toGetAllAccountsWithCardsCommandDto(
      UUID authUserId, Roles role) {
    return new GetAllAccountsWithCardsCommandDto(authUserId, role);
  }
}
