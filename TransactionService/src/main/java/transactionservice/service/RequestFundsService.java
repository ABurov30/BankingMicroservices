package transactionservice.service;

import enums.transaction.TransactionStatus;
import java.util.Map;
import kafkacontracts.transaction.TransactionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.entity.TransactionEntity;
import transactionservice.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
public class RequestFundsService {
  private final TransactionRepository transactionRepository;
  private final TransactionOutboxService transactionOutboxService;

  @Transactional
  public void requestFunds(CreateTransactionCommand command, TransactionEntity transaction) {
    transaction.setStatus(TransactionStatus.FUNDS_REQUESTED);
    transactionRepository.save(transaction);
    transactionOutboxService.saveTransactionOutboxEvent(
        transaction,
        TransactionEventType.TRANSACTION_FUNDS_REQUESTED,
        Map.of(
            "transactionId", transaction.getId(),
            "targetAccountId", transaction.getTargetAccountId(),
            "authUserId", command.sourceAuthUserId()));
  }
}
