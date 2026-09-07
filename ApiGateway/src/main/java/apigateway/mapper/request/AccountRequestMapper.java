package apigateway.mapper.request;

import apigateway.dto.command.account.CheckAccountStatusCommandDto;
import apigateway.dto.command.account.GetAllAccountsWithCardsCommandDto;
import apigateway.dto.request.account.GetAccountByIdRequestDto;
import apigateway.dto.request.account.GetAllAccountsRequestDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountRequestMapper {
  GetAccountByIdRequestDto toGetAccountByIdRequestDto(CheckAccountStatusCommandDto command);

  GetAllAccountsRequestDto toGetAllAccountsRequestDto(GetAllAccountsWithCardsCommandDto command);
}
