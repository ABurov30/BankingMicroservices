package transactionservice.mapper.command;

import enums.account.AccountCurrency;
import enums.transaction.TransactionStatus;
import java.math.BigDecimal;
import java.util.UUID;
import kafkacontracts.account.TransactionCompensatedEventPayload;
import kafkacontracts.account.TransactionCompletedEventPayload;
import org.mapstruct.Mapper;
import transaction.contract.v1.CreateTransactionGrpcRequest;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.MarkAsCommand;

@Mapper(componentModel = "spring")
public interface TransactionCommandMapper {

  default CreateTransactionCommand toCreateTransactionCommand(
      CreateTransactionGrpcRequest grpcRequest) {
    return new CreateTransactionCommand(
        UUID.fromString(grpcRequest.getSourceAccountId()),
        UUID.fromString(grpcRequest.getTargetAccountId()),
        BigDecimal.valueOf(grpcRequest.getAmount()),
        AccountCurrency.valueOf(grpcRequest.getCurrency()),
        UUID.fromString(grpcRequest.getIdempotencyKey()),
        UUID.fromString(grpcRequest.getSourceAuthUserId()),
        UUID.fromString(grpcRequest.getSourceTargetAuthUserId()));
  }

  default MarkAsCommand toMarkAsCommand(TransactionCompensatedEventPayload payload) {
    return new MarkAsCommand(payload.getTransactionId(), TransactionStatus.COMPENSATED);
  }

  default MarkAsCommand toMarkAsCommand(TransactionCompletedEventPayload payload) {
    return new MarkAsCommand(payload.getTransactionId(), TransactionStatus.COMPLETED);
  }
}
