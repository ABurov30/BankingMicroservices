package accountservice.mapper.eventpayload;

import java.util.Map;
import java.util.UUID;
import kafkacontracts.account.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountEventPayloadMapper {
  default AccountCreatedEventPayload toAccountCreatedEventPayload(Map<String, Object> payload) {
    return AccountCreatedEventPayload.newBuilder()
        .setAccountId(UUID.fromString(payload.get("accountId").toString()))
        .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
        .setAccountNumber(payload.get("accountNumber").toString())
        .setCurrency(payload.get("currency").toString())
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

  default TransactionCompletedEventPayload toTransactionCompletedEventPayload(
      Map<String, Object> payload) {
    return TransactionCompletedEventPayload.newBuilder()
        .setAccountNumber(payload.get("accountNumber").toString())
        .setTransactionId(UUID.fromString(payload.get("transactionId").toString()))
        .setAmountMinorUnits(toLong(payload.get("amountMinorUnits")))
        .setCurrency(payload.get("currency").toString())
        .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
        .build();
  }

  default TransactionCompensatedEventPayload toTransactionCompensatedEventPayload(
      Map<String, Object> payload) {
    return TransactionCompensatedEventPayload.newBuilder()
        .setTransactionId(UUID.fromString(payload.get("transactionId").toString()))
        .build();
  }

  private static Long toLong(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Outbox payload field 'amountMinorUnits' is required");
    }

    if (value instanceof Number number) {
      return number.longValue();
    }

    return Long.parseLong(value.toString());
  }
}
