package accountservice.mapper.eventpayload;

import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import org.mapstruct.Mapper;

import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountEventPayloadMapper {
    default AccountCreatedEventPayload toAccountCreatedEventPayload(Map<String, Object> payload) { return AccountCreatedEventPayload.newBuilder().setAccountId(UUID.fromString(payload.get("accountId").toString())).setAuthUserId(UUID.fromString(payload.get("authUserId").toString())).build(); }
    default AccountFrozenEventPayload toAccountFrozenEventPayload(Map<String, Object> payload) { return AccountFrozenEventPayload.newBuilder().setAccountId(UUID.fromString(payload.get("accountId").toString())).build(); }
}
