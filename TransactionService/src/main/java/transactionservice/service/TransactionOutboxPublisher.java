package transactionservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import kafkacontracts.transaction.TransactionEventType;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import outboxsupport.OutboxDispatcher;
import outboxsupport.OutboxProperties;
import transactionservice.entity.TransactionOutboxEventEntity;
import transactionservice.mapper.eventpayload.TransactionEventPayloadMapper;
import transactionservice.repository.TransactionOutboxEventRepository;

@Service
@RequiredArgsConstructor
public class TransactionOutboxPublisher {
  private final OutboxDispatcher<TransactionOutboxEventEntity> dispatcher;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final TransactionEventPayloadMapper eventPayloadMapper;
  private final TransactionOutboxEventRepository repository;
  private final OutboxProperties properties;
  private final MeterRegistry meters;

  @Scheduled(
      fixedDelayString = "#{@outboxProperties.polling().toMillis()}",
      initialDelayString = "#{@outboxProperties.initialDelay().toMillis()}")
  public void publishPendingEvents() {
    dispatcher.dispatch(this::send);
  }

  private CompletionStage<?> send(TransactionOutboxEventEntity event) {
    SpecificRecord payload =
        extractPayload(TransactionEventType.valueOf(event.getEventType()), event.getPayload());
    var message =
        MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, event.getTopic())
            .setHeader(KafkaHeaders.KEY, event.getEventKey())
            .setHeader("eventId", event.getId().toString());

    if (!repository.ownsAttempt(
        event.getId(), event.getLockedBy(), properties.lease().toMillis())) {
      throw new IllegalStateException("Outbox attempt no longer owns its lease: " + event.getId());
    }
    if (event.getRetryCount() > 1) {
      meters.counter("outbox.retry", "outbox", "transaction_outbox_events").increment();
    }
    return kafkaTemplate
        .send(message.build())
        .whenComplete(
            (result, failure) -> {
              if (failure == null) {
                Duration latency = Duration.between(event.getCreatedAt(), LocalDateTime.now());
                if (!latency.isNegative()) {
                  meters
                      .timer("outbox.publication.latency", "outbox", "transaction_outbox_events")
                      .record(latency);
                }
              }
            });
  }

  private SpecificRecord extractPayload(
      TransactionEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case TRANSACTION_FAILED -> eventPayloadMapper.toTransactionFailedEventPayload(payload);
      case TRANSACTION_FUNDS_REQUESTED ->
          eventPayloadMapper.toTransactionFundsRequestedEventPayload(payload);
      case TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION ->
          eventPayloadMapper.toTransactionCardLimitHoldCompensationEventPayload(payload);
    };
  }
}
