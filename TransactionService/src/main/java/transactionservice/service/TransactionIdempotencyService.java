package transactionservice.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.CreateTransactionResult;
import transactionservice.entity.TransactionEntity;
import transactionservice.exception.IdempotencyPayloadMismatchException;
import transactionservice.exception.IdempotentTransactionNotFoundException;
import transactionservice.mapper.result.TransactionResultMapper;
import transactionservice.repository.TransactionRepository;

@Service
public class TransactionIdempotencyService {
  private final TransactionRepository transactionRepository;
  private final TransactionResultMapper transactionResultMapper;

  public TransactionIdempotencyService(
      TransactionRepository transactionRepository,
      TransactionResultMapper transactionResultMapper) {
    this.transactionRepository = transactionRepository;
    this.transactionResultMapper = transactionResultMapper;
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public CreateTransactionResult getExistingTransactionResult(CreateTransactionCommand command) {
    var existedTransaction =
        transactionRepository
            .findByIdempotencyKey(command.idempotencyKey())
            .orElseThrow(
                () -> new IdempotentTransactionNotFoundException(command.idempotencyKey()));

    if (!checkIsSamePayloadOfTransaction(command, existedTransaction)) {
      throw new IdempotencyPayloadMismatchException(command.idempotencyKey());
    }

    return transactionResultMapper.toCreateTransactionResult(existedTransaction);
  }

  public boolean checkIsSamePayloadOfTransaction(
      CreateTransactionCommand command, TransactionEntity transaction) {
    return command.currency().name().equals(transaction.getCurrency().name())
        && command.minorUnits().equals(transaction.getMinorUnits())
        && command.sourceAccountId().equals(transaction.getSourceAccountId())
        && command.targetAccountId().equals(transaction.getTargetAccountId());
  }
}
