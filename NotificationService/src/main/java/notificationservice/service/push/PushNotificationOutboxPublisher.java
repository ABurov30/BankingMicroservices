package notificationservice.service.push;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import kafkacontracts.notification.NotificationEventType;
import lombok.RequiredArgsConstructor;
import notificationservice.entity.PushNotificationOutboxEventEntity;
import notificationservice.mapper.eventpayload.PushNotificationEventPayloadMapper;
import notificationservice.repository.PushNotificationOutboxEventRepository;
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
public class PushNotificationOutboxPublisher {
  private final OutboxDispatcher<PushNotificationOutboxEventEntity> dispatcher;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final PushNotificationEventPayloadMapper eventPayloadMapper;
  private final PushNotificationOutboxEventRepository repository;
  private final OutboxProperties properties;
  private final MeterRegistry meters;

  @Scheduled(
      fixedDelayString = "#{@outboxProperties.polling().toMillis()}",
      initialDelayString = "#{@outboxProperties.initialDelay().toMillis()}")
  public void publishPendingEvents() {
    dispatcher.dispatch(this::send);
  }

  private CompletionStage<?> send(PushNotificationOutboxEventEntity event) {
    SpecificRecord payload =
        extractPayload(NotificationEventType.valueOf(event.getEventType()), event.getPayload());
    var message =
        MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, event.getTopic())
            .setHeader(KafkaHeaders.KEY, event.getEventKey())
            .setHeader("eventId", event.getId().toString());
    // Recheck after payload mapping, before entering the Kafka producer.
    if (!repository.ownsAttempt(
        event.getId(), event.getLockedBy(), properties.lease().toMillis())) {
      throw new IllegalStateException("Outbox attempt no longer owns its lease: " + event.getId());
    }
    if (event.getRetryCount() > 1) {
      meters.counter("outbox.retry", "outbox", "push_notification_outbox_events").increment();
    }
    return kafkaTemplate
        .send(message.build())
        .whenComplete(
            (result, failure) -> {
              if (failure == null) {
                Duration latency = Duration.between(event.getCreatedAt(), LocalDateTime.now());
                if (!latency.isNegative()) {
                  meters
                      .timer(
                          "outbox.publication.latency", "outbox", "push_notification_outbox_events")
                      .record(latency);
                }
              }
            });
  }

  private SpecificRecord extractPayload(
      NotificationEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case PUSH_NOTIFICATION_CREATED ->
          eventPayloadMapper.toPushNotificationCreatedEventPayload(payload);
    };
  }
}
