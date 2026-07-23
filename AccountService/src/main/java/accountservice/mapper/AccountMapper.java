package accountservice.mapper;

import account.contract.v1.*;
import accountservice.dto.CreateAccountCommand;
import accountservice.dto.CreateAccountResult;
import accountservice.dto.GetAccountResult;
import accountservice.dto.GetAccountsByOwnerUserIdCommand;
import accountservice.entity.AccountEntity;
import accountservice.grpc.AccountGrpcService;
import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        imports = {
                AccountType.class,
                AccountStatus.class,
                AccountCurrency.class
        }
)
public interface AccountMapper {

    @Mapping(target = "type", expression = "java(AccountType.CHECKING)")
    @Mapping(target = "status", expression = "java(AccountStatus.ACTIVE)")
    @Mapping(target = "currency", expression = "java(AccountCurrency.RUB)")
    CreateAccountCommand toCreateAccountCommand(UserProfileCreatedEventPayload userProfileCreatedEventPayload);

    default AccountResponse toAccountResponse (GetAccountResult getAccountResult) {
        return  AccountResponse.newBuilder()
                .setAccountId(getAccountResult.accountId().toString())
                .setOwnerUserId(getAccountResult.ownerUserId().toString())
                .setAccountNumber(getAccountResult.accountNumber())
                .setType(getAccountResult.type().name())
                .setStatus(getAccountResult.status().name())
                .setAvailableBalance(getAccountResult.availableBalance().longValue())
                .setReservedBalance(getAccountResult.reservedBalance().longValue())
                .setCurrency(getAccountResult.currency().name())
                .build();
    }

    default AccountResponse toAccountResponse (CreateAccountResult createAccountResult) {
        return  AccountResponse.newBuilder()
                .setAccountId(createAccountResult.accountId().toString())
                .setOwnerUserId(createAccountResult.ownerUserId().toString())
                .setAccountNumber(createAccountResult.accountNumber())
                .setType(createAccountResult.type().name())
                .setStatus(createAccountResult.status().name())
                .setAvailableBalance(createAccountResult.availableBalance().longValue())
                .setReservedBalance(createAccountResult.reservedBalance().longValue())
                .setCurrency(createAccountResult.currency().name())
                .build();
    }

    default CreateAccountCommand toCreateAccountCommand(CreateAccountGrpcRequest createAccountGrpcRequest) {
        return new CreateAccountCommand(
                UUID.fromString(createAccountGrpcRequest.getOwnerUserId()),
                AccountType.valueOf(createAccountGrpcRequest.getType()),
                AccountCurrency.valueOf(createAccountGrpcRequest.getCurrency())
        );
    }

    default CreateAccountGrpcResponse toCreateAccountGrpcResponse(CreateAccountResult createAccountResult) {
        AccountResponse accountResponse = this.toAccountResponse(createAccountResult);
        return CreateAccountGrpcResponse.newBuilder()
                .setAccount(accountResponse)
                .build();
    }

    default GetAccountsByOwnerUserIdCommand toGetAccountsByOwnerUserIdCommand (GetAccountByOwnerUserIdGrpcRequest request) {
        return new GetAccountsByOwnerUserIdCommand(UUID.fromString(request.getOwnerUserId()));
    }

    default GetAccountsGrpcResponse toGetAccountsGrpcResponse (List<AccountResponse> accountResponseList) {
        return GetAccountsGrpcResponse.newBuilder()
                .addAllAccounts(accountResponseList)
                .build();
    }

    GetAccountResult toGetAccountResult (AccountEntity accountEntity);
}
