package transactionservice.listener;

import kafkacontracts.account.TransactionCompensatedEventPayload;
import kafkacontracts.account.TransactionCompletedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import processedevent.annotation.EventKey;
import processedevent.annotation.IdempotentKafkaEvent;
import transactionservice.mapper.command.TransactionCommandMapper;
import transactionservice.service.TransactionService;

@Component
@RequiredArgsConstructor
public class TransactionKafkaListener {
  private final TransactionService transactionService;
  private final TransactionCommandMapper transactionCommandMapper;

  @IdempotentKafkaEvent
  @KafkaListener(
      topics = "#{T(kafkacontracts.account.AccountEventType).TRANSACTION_COMPLETED.getTopic()}")
  public void handleTransactionCompleted(
      TransactionCompletedEventPayload payload,
      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
    transactionService.markAs(transactionCommandMapper.toMarkAsCommand(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(
      topics = "#{T(kafkacontracts.account.AccountEventType).TRANSACTION_COMPENSATED.getTopic()}")
  public void handleTransactionCompensated(
      TransactionCompensatedEventPayload payload,
      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
    transactionService.markAs(transactionCommandMapper.toMarkAsCommand(payload));
  }
}
