package transactionservice.service;

import java.util.Map;
import kafkacontracts.transaction.TransactionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import transactionservice.entity.TransactionEntity;
import transactionservice.entity.TransactionOutboxEventEntity;
import transactionservice.repository.TransactionOutboxEventRepository;

@Service
@RequiredArgsConstructor
public class TransactionOutboxService {
  private final TransactionOutboxEventRepository transactionOutboxEventRepository;

  public TransactionOutboxEventEntity saveTransactionOutboxEvent(
      TransactionEntity transaction, TransactionEventType eventType, Map<String, Object> payload) {
    var transactionOutboxEvent =
        TransactionOutboxEventFactory.create(transaction.getId(), eventType);
    transactionOutboxEvent.setPayload(payload);
    return transactionOutboxEventRepository.save(transactionOutboxEvent);
  }
}
