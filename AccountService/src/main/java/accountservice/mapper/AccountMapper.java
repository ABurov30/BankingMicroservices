package accountservice.mapper;

import account.contract.v1.CreateAccountGrpcRequest;
import account.contract.v1.CreateAccountGrpcResponse;
import accountservice.dto.CreateAccountCommand;
import accountservice.dto.CreateAccountResult;
import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

    default CreateAccountCommand toCreateAccountCommand(CreateAccountGrpcRequest createAccountGrpcRequest) {
        return new CreateAccountCommand(
                UUID.fromString(createAccountGrpcRequest.getOwnerUserId()),
                AccountType.valueOf(createAccountGrpcRequest.getType()),
                AccountCurrency.valueOf(createAccountGrpcRequest.getCurrency())
        );
    }

    default CreateAccountGrpcResponse toCreateAccountGrpcResponse(CreateAccountResult createAccountResult) {
        return CreateAccountGrpcResponse.newBuilder()
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
}
