package accountservice.mapper.eventpayload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
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

  private static ByteBuffer toAvroDecimal(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Outbox payload field 'amount' is required");
    }

    BigDecimal amount =
        value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());

    BigDecimal scaledAmount = amount.setScale(2, RoundingMode.UNNECESSARY);

    if (scaledAmount.precision() > 19) {
      throw new IllegalArgumentException("Amount exceeds Avro decimal(19,2): " + amount);
    }

    return ByteBuffer.wrap(scaledAmount.unscaledValue().toByteArray());
  }

  default TransactionCompletedEventPayload toTransactionCompletedEventPayload(
      Map<String, Object> payload) {
    return TransactionCompletedEventPayload.newBuilder()
        .setAccountNumber(payload.get("accountNumber").toString())
        .setTransactionId(UUID.fromString(payload.get("transactionId").toString()))
        .setAmount(toAvroDecimal(payload.get("amount")))
        .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
        .build();
  }

  default TransactionCompensatedEventPayload toTransactionCompensatedEventPayload(
      Map<String, Object> payload) {
    return TransactionCompensatedEventPayload.newBuilder()
        .setTransactionId(UUID.fromString(payload.get("transactionId").toString()))
        .build();
  }
}
