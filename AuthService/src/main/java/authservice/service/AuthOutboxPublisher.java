package authservice.service;

import authservice.entity.AuthOutboxEventEntity;
import authservice.mapper.eventpayload.AuthEventPayloadMapper;
import authservice.repository.AuthOutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import kafkacontracts.auth.AuthEventType;
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
public class AuthOutboxPublisher {
  private final OutboxDispatcher<AuthOutboxEventEntity> dispatcher;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final AuthEventPayloadMapper eventPayloadMapper;
  private final AuthOutboxEventRepository repository;
  private final OutboxProperties properties;
  private final MeterRegistry meters;

  @Scheduled(
      fixedDelayString = "#{@outboxProperties.polling().toMillis()}",
      initialDelayString = "#{@outboxProperties.initialDelay().toMillis()}")
  public void publishPendingEvents() {
    dispatcher.dispatch(this::send);
  }

  private CompletionStage<?> send(AuthOutboxEventEntity event) {
    SpecificRecord payload =
        extractPayload(AuthEventType.valueOf(event.getEventType()), event.getPayload());
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
      meters.counter("outbox.retry", "outbox", "auth_outbox_events").increment();
    }
    return kafkaTemplate
        .send(message.build())
        .whenComplete(
            (result, failure) -> {
              if (failure == null) {
                Duration latency = Duration.between(event.getCreatedAt(), LocalDateTime.now());
                if (!latency.isNegative()) {
                  meters
                      .timer("outbox.publication.latency", "outbox", "auth_outbox_events")
                      .record(latency);
                }
              }
            });
  }

  private SpecificRecord extractPayload(AuthEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case AUTH_USER_CREATED -> eventPayloadMapper.toAuthUserCreatedEventPayload(payload);
      case AUTH_USER_BLOCKED -> eventPayloadMapper.toAuthUserBlockedEventPayload(payload);
      case AUTH_USER_UNLOCK -> eventPayloadMapper.toAuthUserUnlockEventPayload(payload);
      case AUTH_USER_VERIFIED -> eventPayloadMapper.toAuthUserVerifiedEventPayload(payload);
      case AUTH_USER_ROLE_CHANGED -> eventPayloadMapper.toAuthUserRoleChangedEventPayload(payload);
      case AUTH_USER_FORGET_PASSWORD ->
          eventPayloadMapper.toAuthUserForgetPasswordEventPayload(payload);
      case AUTH_SOCIAL_ACCOUNT_AUTH_USER_CREATED ->
          eventPayloadMapper.toAuthSocialAccountAuthUserCreatedEventPayload(payload);
    };
  }
}
