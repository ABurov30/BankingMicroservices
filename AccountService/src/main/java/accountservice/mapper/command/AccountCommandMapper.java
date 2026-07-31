package accountservice.mapper.command;

import account.contract.v1.CreateAccountGrpcRequest;
import account.contract.v1.FreezeAccountGrpcRequest;
import account.contract.v1.GetAccountByOwnerUserIdGrpcRequest;
import accountservice.dto.CreateAccountCommand;
import accountservice.dto.FreezeAccountCommand;
import accountservice.dto.FreezeAccountsByUserIdCommand;
import accountservice.dto.GetAccountsByOwnerUserIdCommand;
import enums.account.AccountCurrency;
import enums.account.AccountType;
import kafkacontracts.user.UserProfileBlockedEventPayload;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountCommandMapper {
    default CreateAccountCommand toCreateAccountCommand(UserProfileCreatedEventPayload payload) { return new CreateAccountCommand(payload.getUserId(), payload.getAuthUserId(), AccountType.CHECKING, AccountCurrency.RUB); }
    default CreateAccountCommand toCreateAccountCommand(CreateAccountGrpcRequest request) { return new CreateAccountCommand(UUID.fromString(request.getOwnerUserId()), UUID.fromString(request.getAuthUserId()), AccountType.valueOf(request.getType()), AccountCurrency.valueOf(request.getCurrency())); }
    default GetAccountsByOwnerUserIdCommand toGetAccountsByOwnerUserIdCommand(GetAccountByOwnerUserIdGrpcRequest request) { return new GetAccountsByOwnerUserIdCommand(UUID.fromString(request.getOwnerUserId())); }
    default FreezeAccountCommand toFreezeAccountCommand(FreezeAccountGrpcRequest request) { return new FreezeAccountCommand(UUID.fromString(request.getAccountId()), UUID.fromString(request.getAuthUserId()), request.getRole()); }
    default FreezeAccountsByUserIdCommand toFreezeAccountsByUserIdCommand(UserProfileBlockedEventPayload payload) { return new FreezeAccountsByUserIdCommand(payload.getUserId()); }
}
