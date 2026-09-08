package apigateway.mapper.request;

import apigateway.dto.command.account.*;
import apigateway.dto.command.transaction.GetTransactionByUserIdCommandDto;
import apigateway.dto.request.account.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountRequestMapper {
  GetAccountByIdRequestDto toGetAccountByIdRequestDto(CheckAccountStatusCommandDto command);

  GetAllAccountsRequestDto toGetAllAccountsRequestDto(GetAllAccountsWithCardsCommandDto command);

  GetAccountsWithCardsByOwnerIdRequestDto toGetAccountsWithCardsByOwnerIdRequestDto(
      GetAccountsWithCardsByOwnerIdCommandDto command);

  GetAccountsWithCardsByOwnerIdRequestDto toGetAccountsWithCardsByOwnerIdRequestDto(
      GetTransactionByUserIdCommandDto command);

  GetAccountsByAuthUserIdRequestDto toGetAccountsByAuthUserIdRequestDto(
      GetAllAccountsWithCardsByAuthUserIdCommandDto command);

  CreateAccountRequestDto toCreateAccountRequestDto(CreateAccountCommandDto commandDto);
}
