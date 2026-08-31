package transactionservice.mapper.eventpayload;

import java.util.Map;
import java.util.UUID;
import kafkacontracts.account.TransactionFailedEventPayload;
import kafkacontracts.account.TransactionFundsRequestedEventPayload;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionEventPayloadMapper {

  default TransactionFailedEventPayload toTransactionFailedEventPayload(
      Map<String, Object> payload) {
    return TransactionFailedEventPayload.newBuilder()
        .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
        .setAmountMinorUnits(toLong(payload.get("amountMinorUnits")))
        .setCurrency(payload.get("currency").toString())
        .build();
  }

  default TransactionFundsRequestedEventPayload toTransactionFundsRequestedEventPayload(
      Map<String, Object> payload) {
    return TransactionFundsRequestedEventPayload.newBuilder()
        .setTransactionId(UUID.fromString(payload.get("transactionId").toString()))
        .setAuthUserId(UUID.fromString(payload.get("authUserId").toString()))
        .setTargetAccountId(UUID.fromString(payload.get("targetAccountId").toString()))
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
