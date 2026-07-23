package apigateway.mapper;

import account.contract.v1.CreateAccountGrpcRequest;
import account.contract.v1.CreateAccountGrpcResponse;
import account.contract.v1.AccountResponse;
import account.contract.v1.GetAccountsGrpcResponse;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.CreateAccountResponseDto;
import apigateway.dto.account.GetAccountResponseDto;
import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    default CreateAccountGrpcRequest toCreateAccountGrpcRequest(CreateAccountRequestDto request) {
        return CreateAccountGrpcRequest.newBuilder()
                .setOwnerUserId(request.ownerUserId().toString())
                .setType(request.type().name())
                .setCurrency(request.currency().name())
                .build();
    }

    default CreateAccountResponseDto toCreateAccountResponseDto(CreateAccountGrpcResponse response) {
        AccountResponse account = response.getAccount();

        return new CreateAccountResponseDto(
                UUID.fromString(account.getAccountId()),
                UUID.fromString(account.getOwnerUserId()),
                account.getAccountNumber(),
                AccountType.valueOf(account.getType()),
                AccountStatus.valueOf(account.getStatus()),
                BigDecimal.valueOf(account.getAvailableBalance(), 2),
                BigDecimal.valueOf(account.getReservedBalance(), 2),
                AccountCurrency.valueOf(account.getCurrency())
        );
    }

    default GetAccountResponseDto toGetAccountResponseDto(AccountResponse account) {
        return new GetAccountResponseDto(
                UUID.fromString(account.getAccountId()),
                UUID.fromString(account.getOwnerUserId()),
                account.getAccountNumber(),
                AccountType.valueOf(account.getType()),
                AccountStatus.valueOf(account.getStatus()),
                BigDecimal.valueOf(account.getAvailableBalance(), 2),
                BigDecimal.valueOf(account.getReservedBalance(), 2),
                AccountCurrency.valueOf(account.getCurrency())
        );
    }

    default List<GetAccountResponseDto> toListGetAccountResponseDto (GetAccountsGrpcResponse response) {
        return response.getAccountsList().stream().map(this::toGetAccountResponseDto).toList();
    }
}
