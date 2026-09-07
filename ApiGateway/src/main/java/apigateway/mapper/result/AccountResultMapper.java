package apigateway.mapper.result;

import account.contract.v1.AccountResponse;
import account.contract.v1.CreateAccountGrpcResponse;
import account.contract.v1.GetAccountByIdGrpcResponse;
import account.contract.v1.GetAccountsGrpcResponse;
import account.contract.v1.GetRecipientAccountsByOwnerUserIdGrpcResponse;
import account.contract.v1.RecipientAccount;
import apigateway.dto.response.account.AccountResponseWithoutSensitiveInfo;
import apigateway.dto.response.account.CreateAccountResponseDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.common.Currency;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountResultMapper {
  default CreateAccountResponseDto toCreateAccountResponseDto(CreateAccountGrpcResponse response) {
    AccountResponse account = response.getAccount();
    return new CreateAccountResponseDto(
        UUID.fromString(account.getAccountId()),
        UUID.fromString(account.getOwnerUserId()),
        account.getAccountNumber(),
        AccountType.valueOf(account.getType()),
        AccountStatus.valueOf(account.getStatus()),
        account.getAvailableBalanceMinorUnits(),
        account.getReservedBalanceMinorUnits(),
        Currency.valueOf(account.getCurrency()));
  }

  default GetAccountResponseDto toGetAccountResponseDto(AccountResponse account) {
    return new GetAccountResponseDto(
        UUID.fromString(account.getAccountId()),
        UUID.fromString(account.getOwnerUserId()),
        account.getAccountNumber(),
        AccountType.valueOf(account.getType()),
        AccountStatus.valueOf(account.getStatus()),
        account.getAvailableBalanceMinorUnits(),
        account.getReservedBalanceMinorUnits(),
        Currency.valueOf(account.getCurrency()));
  }

  default GetAccountResponseDto toGetAccountByIdResponseDto(GetAccountByIdGrpcResponse response) {
    return toGetAccountResponseDto(response.getAccount());
  }

  default List<GetAccountResponseDto> toListGetAccountResponseDto(
      GetAccountsGrpcResponse response) {
    return response.getAccountsList().stream().map(this::toGetAccountResponseDto).toList();
  }

  default List<AccountResponseWithoutSensitiveInfo> toRecipientAccounts(
      GetRecipientAccountsByOwnerUserIdGrpcResponse response) {
    return response.getAccountsList().stream().map(this::toRecipientAccount).toList();
  }

  default AccountResponseWithoutSensitiveInfo toRecipientAccount(RecipientAccount account) {
    return new AccountResponseWithoutSensitiveInfo(
        UUID.fromString(account.getAccountId()),
        account.getAccountNumberLast4Chars(),
        AccountType.valueOf(account.getType()),
        AccountStatus.valueOf(account.getStatus()),
        Currency.valueOf(account.getCurrency()));
  }
}
