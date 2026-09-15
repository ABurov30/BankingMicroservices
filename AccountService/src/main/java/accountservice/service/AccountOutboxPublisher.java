package accountservice.service;

import accountservice.entity.AccountOutboxEventEntity;
import accountservice.mapper.eventpayload.AccountEventPayloadMapper;
import accountservice.repository.AccountOutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import kafkacontracts.account.AccountEventType;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import outboxsupport.OutboxDispatcher;
import outboxsupport.OutboxProperties;

@Service
@RequiredArgsConstructor
public class AccountOutboxPublisher {
  public static final String TRANSACTION_NOTIFICATION_DIRECTION_HEADER =
      "transaction-notification-direction";
  private final OutboxDispatcher<AccountOutboxEventEntity> dispatcher;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final AccountEventPayloadMapper eventPayloadMapper;
  private final AccountOutboxEventRepository repository;
  private final OutboxProperties properties;
  private final MeterRegistry meters;

  @Scheduled(
      fixedDelayString = "#{@outboxProperties.polling().toMillis()}",
      initialDelayString = "#{@outboxProperties.initialDelay().toMillis()}")
  public void publishPendingEvents() {
    dispatcher.dispatch(this::send);
  }

  private CompletionStage<?> send(AccountOutboxEventEntity event) {
    SpecificRecord payload =
        extractPayload(AccountEventType.valueOf(event.getEventType()), event.getPayload());
    var message =
        MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, event.getTopic())
            .setHeader(KafkaHeaders.KEY, event.getEventKey())
            .setHeader("eventId", event.getId().toString());
    if (event.getPayload().containsKey("transactionDirection")) {
      message.setHeader(
          TRANSACTION_NOTIFICATION_DIRECTION_HEADER,
          event.getPayload().get("transactionDirection").toString());
    }
    // Recheck after payload mapping, before entering the Kafka producer.
    if (!repository.ownsAttempt(
        event.getId(), event.getLockedBy(), properties.lease().toMillis())) {
      throw new IllegalStateException("Outbox attempt no longer owns its lease: " + event.getId());
    }
    if (event.getRetryCount() > 1) {
      meters.counter("outbox.retry", "outbox", "account_outbox_events").increment();
    }
    return kafkaTemplate
        .send(message.build())
        .whenComplete(
            (result, failure) -> {
              if (failure == null) {
                Duration latency = Duration.between(event.getCreatedAt(), LocalDateTime.now());
                if (!latency.isNegative()) {
                  meters
                      .timer("outbox.publication.latency", "outbox", "account_outbox_events")
                      .record(latency);
                }
              }
            });
  }

  private SpecificRecord extractPayload(AccountEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case ACCOUNT_CREATED -> eventPayloadMapper.toAccountCreatedEventPayload(payload);
      case ACCOUNT_FROZEN -> eventPayloadMapper.toAccountFrozenEventPayload(payload);
      case ACCOUNT_UNFROZEN -> eventPayloadMapper.toAccountUnfrozenEventPayload(payload);
      case TRANSACTION_COMPENSATED ->
          eventPayloadMapper.toTransactionCompensatedEventPayload(payload);
      case TRANSACTION_COMPLETED -> eventPayloadMapper.toTransactionCompletedEventPayload(payload);
      case ACCOUNT_HOLD_RELEASED_BY_TIME ->
          eventPayloadMapper.toAccountHoldReleasedByTimeEventPayload(payload);
    };
  }
}
