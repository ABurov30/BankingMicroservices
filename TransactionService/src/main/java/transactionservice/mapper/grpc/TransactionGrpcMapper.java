package transactionservice.mapper.grpc;

import account.contract.v1.AccountResponse;
import account.contract.v1.AccountResponseWithoutSensitiveInfo;
import account.contract.v1.ReserveFundsForTransactionGrpcRequest;
import card.contract.v1.ReserveLimitsForTransactionGrpcRequest;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.mapstruct.Mapper;
import transaction.contract.v1.TransactionResponse;
import transaction.contract.v1.TransactionStatusResponse;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.entity.TransactionEntity;

@Mapper(componentModel = "spring")
public interface TransactionGrpcMapper {
  default ReserveFundsForTransactionGrpcRequest toReserveFundsForTransactionGrpcRequest(
      TransactionEntity transactionEntity, UUID sourceAuthUserId) {
    return ReserveFundsForTransactionGrpcRequest.newBuilder()
        .setTransactionId(transactionEntity.getId().toString())
        .setMinorUnits(transactionEntity.getMinorUnits().longValue())
        .setSourceAccountId(transactionEntity.getSourceAccountId().toString())
        .setTargetAccountId(transactionEntity.getTargetAccountId().toString())
        .setSourceAuthUserId(sourceAuthUserId.toString())
        .build();
  }

  default ReserveLimitsForTransactionGrpcRequest toReserveLimitsForTransactionGrpcRequest(
      TransactionEntity transactionEntity, CreateTransactionCommand command) {
    return ReserveLimitsForTransactionGrpcRequest.newBuilder()
        .setTransactionId(transactionEntity.getId().toString())
        .setMinorUnits(transactionEntity.getMinorUnits().longValue())
        .setCurrency(transactionEntity.getCurrency().name())
        .setSourceAuthUserId(command.sourceAuthUserId().toString())
        .setSourceCardId(command.sourceCardId().toString())
        .build();
  }

  default TransactionResponse toTransactionResponse(
      TransactionEntity transaction, AccountResponse sourceAccount, AccountResponse targetAccount) {
    var response =
        TransactionResponse.newBuilder()
            .setTransactionId(transaction.getId().toString())
            .setMinorUnits(transaction.getMinorUnits().longValue())
            .setCurrency(transaction.getCurrency().name())
            .setStatus(transaction.getStatus().name());

    if (transaction.getCreatedAt() != null) {
      response.setCreatedAt(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(transaction.getCreatedAt()));
    }
    if (transaction.getCompletedAt() != null) {
      response.setCompletedAt(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(transaction.getCompletedAt()));
    }

    if (sourceAccount != null) {
      response.setSourceAccount(sourceAccount);
    }

    if (targetAccount != null) {
      response.setTargetAccount(targetAccount);
    }

    return response.build();
  }

  default TransactionStatusResponse toTransactionStatusResponse(
      TransactionEntity transaction, AccountResponse sourceAccount, AccountResponse targetAccount) {
    var response =
        TransactionStatusResponse.newBuilder()
            .setMinorUnits(transaction.getMinorUnits().longValue())
            .setCurrency(transaction.getCurrency().name())
            .setStatus(transaction.getStatus().name());

    if (sourceAccount != null) {
      response.setSourceAccount(toAccountResponseWithoutSensitiveInfo(sourceAccount));
    }

    if (targetAccount != null) {
      response.setTargetAccount(toAccountResponseWithoutSensitiveInfo(targetAccount));
    }

    return response.build();
  }

  private AccountResponseWithoutSensitiveInfo toAccountResponseWithoutSensitiveInfo(
      AccountResponse account) {
    return AccountResponseWithoutSensitiveInfo.newBuilder()
        .setAccountNumber(account.getAccountNumber())
        .setCurrency(account.getCurrency())
        .build();
  }
}
