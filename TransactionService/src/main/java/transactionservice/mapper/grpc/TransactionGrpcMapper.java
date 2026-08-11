package transactionservice.mapper.grpc;

import account.contract.v1.ReserveFundsForTransactionGrpcRequest;
import org.mapstruct.Mapper;
import transactionservice.entity.TransactionEntity;

@Mapper(componentModel = "spring")
public interface TransactionGrpcMapper {
    default ReserveFundsForTransactionGrpcRequest toReserveFundsForTransactionGrpcRequest (TransactionEntity transactionEntity) {
        return  ReserveFundsForTransactionGrpcRequest.newBuilder()
                .setTransactionId(transactionEntity.getId().toString())
                .setAmount(transactionEntity.getAmount().longValue())
                .setSourceAccountId(transactionEntity.getSourceAccountId().toString())
                .setTargetAccountId(transactionEntity.getTargetAccountId().toString())
                .build();
    }
}
