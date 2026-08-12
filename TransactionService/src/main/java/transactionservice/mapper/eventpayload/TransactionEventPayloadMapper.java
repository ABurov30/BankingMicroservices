package transactionservice.mapper.eventpayload;

import java.nio.ByteBuffer;
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
        .setAccountNumber(payload.get("accountNumber").toString())
        .setAmount((ByteBuffer) payload.get("amount"))
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
}
