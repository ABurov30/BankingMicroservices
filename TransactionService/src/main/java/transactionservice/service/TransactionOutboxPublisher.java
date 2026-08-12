package transactionservice.service;

import java.util.List;
import java.util.Map;
import kafkacontracts.transaction.TransactionEventType;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;
import transactionservice.entity.TransactionOutboxEventEntity;
import transactionservice.mapper.eventpayload.TransactionEventPayloadMapper;
import transactionservice.repository.TransactionOutboxEventRepository;

@Service
public class TransactionOutboxPublisher implements KafkaOnSentHandler {
  private static final Logger log = LoggerFactory.getLogger(TransactionOutboxPublisher.class);
  private final TransactionOutboxEventRepository transactionOutboxEventRepository;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final TransactionEventPayloadMapper eventPayloadMapper;

  public TransactionOutboxPublisher(
      TransactionOutboxEventRepository transactionOutboxEventRepository,
      KafkaTemplate<String, SpecificRecord> kafkaTemplate,
      TransactionEventPayloadMapper eventPayloadMapper) {
    this.transactionOutboxEventRepository = transactionOutboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.eventPayloadMapper = eventPayloadMapper;
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publishPendingEvents() {
    List<TransactionOutboxEventEntity> eventEntityList =
        transactionOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING);

    for (TransactionOutboxEventEntity event : eventEntityList) {
      try {
        SpecificRecord payload =
            extractPayload(TransactionEventType.valueOf(event.getEventType()), event.getPayload());

        kafkaTemplate
            .send(event.getTopic(), event.getEventKey(), payload)
            .whenComplete(
                (result, ex) -> {
                  if (ex == null) {
                    onPublish(event.getId(), transactionOutboxEventRepository);
                  } else {
                    onFailed(event.getId(), ex, transactionOutboxEventRepository);
                  }
                });
      } catch (Exception e) {
        log.error(
            "Unable to publish transaction outbox event: eventId={}, eventType={}",
            event.getId(),
            event.getEventType(),
            e);
        onFailed(event.getId(), e, transactionOutboxEventRepository);
      }
    }
  }

  private SpecificRecord extractPayload(
      TransactionEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case TRANSACTION_FAILED -> eventPayloadMapper.toTransactionFailedEventPayload(payload);
      case TRANSACTION_FUNDS_REQUESTED ->
          eventPayloadMapper.toTransactionFundsRequestedEventPayload(payload);
    };
  }
}
