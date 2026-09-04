package transactionservice.mapper.result;

import org.mapstruct.Mapper;
import transactionservice.dto.CreateTransactionResult;
import transactionservice.entity.TransactionEntity;

@Mapper(componentModel = "spring")
public interface TransactionResultMapper {
  default CreateTransactionResult toCreateTransactionResult(TransactionEntity transaction) {
    return new CreateTransactionResult(
        transaction.getId(),
        transaction.getMinorUnits(),
        transaction.getCurrency(),
        transaction.getStatus());
  }
}
