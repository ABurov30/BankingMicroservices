package transactionservice.mapper.grpc;

import account.contract.v1.AccountResponse;
import account.contract.v1.ReserveFundsForTransactionGrpcRequest;
import org.mapstruct.Mapper;
import transaction.contract.v1.TransactionResponse;
import transactionservice.entity.TransactionEntity;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransactionGrpcMapper {
    default ReserveFundsForTransactionGrpcRequest toReserveFundsForTransactionGrpcRequest (TransactionEntity transactionEntity, UUID sourceAuthUserId) {
        return  ReserveFundsForTransactionGrpcRequest.newBuilder()
                .setTransactionId(transactionEntity.getId().toString())
                .setAmount(transactionEntity.getAmount().longValue())
                .setSourceAccountId(transactionEntity.getSourceAccountId().toString())
                .setTargetAccountId(transactionEntity.getTargetAccountId().toString())
                .setSourceAuthUserId(sourceAuthUserId.toString())
                .build();
    }

    default TransactionResponse toTransactionResponse(
            TransactionEntity transaction,
            AccountResponse sourceAccount,
            AccountResponse targetAccount
    ) {
        var response = TransactionResponse.newBuilder()
                .setAmount(transaction.getAmount().longValue())
                .setCurrency(transaction.getCurrency().name())
                .setStatus(transaction.getStatus().name());

        if (transaction.getCreatedAt() != null) {
            response.setCreatedAt(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(transaction.getCreatedAt()));
        }
        if (transaction.getCompletedAdt() != null) {
            response.setCompletedAt(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(transaction.getCompletedAdt()));
        }

        if (sourceAccount != null) {
            response.setSourceAccount(sourceAccount);
        }

        if (targetAccount != null) {
            response.setTargetAccount(targetAccount);
        }

        return response.build();
    }
}
