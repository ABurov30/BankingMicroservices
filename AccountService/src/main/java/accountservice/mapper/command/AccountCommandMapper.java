package accountservice.mapper.command;

import account.contract.v1.*;
import accountservice.dto.*;
import accountservice.entity.AccountHoldEntity;
import enums.account.AccountCurrency;
import enums.account.AccountType;
import kafkacontracts.account.TransactionFundsRequestedEventPayload;
import kafkacontracts.user.UserProfileBlockedEventPayload;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountCommandMapper {
    default CreateAccountCommand toCreateAccountCommand(UserProfileCreatedEventPayload payload) {
        return new CreateAccountCommand(payload.getUserId(), payload.getAuthUserId(), AccountType.CHECKING, AccountCurrency.USD);
    }

    default CreateAccountCommand toCreateAccountCommand(CreateAccountGrpcRequest request) {
        return new CreateAccountCommand(UUID.fromString(request.getOwnerUserId()), UUID.fromString(request.getAuthUserId()), AccountType.valueOf(request.getType()), AccountCurrency.valueOf(request.getCurrency()));
    }

    default GetAccountsByOwnerUserIdCommand toGetAccountsByOwnerUserIdCommand(GetAccountByOwnerUserIdGrpcRequest request) {
        return new GetAccountsByOwnerUserIdCommand(UUID.fromString(request.getOwnerUserId()));
    }

    default FreezeAccountCommand toFreezeAccountCommand(FreezeAccountGrpcRequest request) {
        return new FreezeAccountCommand(UUID.fromString(request.getAccountId()), UUID.fromString(request.getAuthUserId()), request.getRole());
    }

    default UnfreezeAccountCommand toUnfreezeAccountCommand(UnfreezeAccountGrpcRequest request) {
        return new UnfreezeAccountCommand(UUID.fromString(request.getAccountId()), UUID.fromString(request.getAuthUserId()), request.getRole());
    }

    default FreezeAccountsByUserIdCommand toFreezeAccountsByUserIdCommand(UserProfileBlockedEventPayload payload) {
        return new FreezeAccountsByUserIdCommand(payload.getUserId());
    }

    default GetAccountByIdCommand toGetAccountByIdCommand(GetAccountByIdGrpcRequest request) {
        return new GetAccountByIdCommand(UUID.fromString(request.getAccountId()));
    }

    default UpdateAccountBalanceCommand toUpdateAccountBalanceCommand(UpdateAccountBalanceGrpcRequest request) {
        return new UpdateAccountBalanceCommand(UUID.fromString(request.getAccountId()), BigDecimal.valueOf(request.getAmount()));
    }

    default TransactionFundsRequestCommand toTransactionFundsRequestCommand (TransactionFundsRequestedEventPayload payload) {
        return new TransactionFundsRequestCommand(
                payload.getTransactionId(),
                payload.getTargetAccountId(),
                payload.getAuthUserId()
        );
    }
}
