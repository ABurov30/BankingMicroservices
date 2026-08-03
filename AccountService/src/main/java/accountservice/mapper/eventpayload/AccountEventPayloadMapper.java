package accountservice.mapper.eventpayload;

import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import kafkacontracts.account.AccountUnfrozenEventPayload;
import org.mapstruct.Mapper;

import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountEventPayloadMapper {
    default AccountCreatedEventPayload toAccountCreatedEventPayload(Map<String, Object> payload) {
        return AccountCreatedEventPayload.newBuilder()
                .setAccountId(UUID.fromString(payload.get("accountId").toString()))
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .setAccountNumber(payload.get("accountNumber").toString())
                .build();
    }

    default AccountFrozenEventPayload toAccountFrozenEventPayload(Map<String, Object> payload) {
        return AccountFrozenEventPayload.newBuilder()
                .setAccountId(UUID.fromString(payload.get("accountId").toString()))
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .setAccountNumber(payload.get("accountNumber").toString())
                .build();
    }

    default AccountUnfrozenEventPayload toAccountUnfrozenEventPayload(Map<String, Object> payload) {
        return AccountUnfrozenEventPayload.newBuilder()
                .setAccountId(UUID.fromString(payload.get("accountId").toString()))
                .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
                .setAccountNumber(payload.get("accountNumber").toString())
                .build();
    }
}
