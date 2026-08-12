package notificationservice.service.push;

import java.util.List;
import java.util.Map;
import kafkacontracts.notification.NotificationEventType;
import notificationservice.entity.PushNotificationOutboxEventEntity;
import notificationservice.mapper.eventpayload.PushNotificationEventPayloadMapper;
import notificationservice.repository.PushNotificationOutboxEventRepository;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;

@Service
public class PushNotificationOutboxPublisher implements KafkaOnSentHandler {
  private static final Logger log = LoggerFactory.getLogger(PushNotificationOutboxPublisher.class);
  private final PushNotificationOutboxEventRepository pushNotificationOutboxEventRepository;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final PushNotificationEventPayloadMapper eventPayloadMapper;

  public PushNotificationOutboxPublisher(
      PushNotificationOutboxEventRepository pushNotificationOutboxEventRepository,
      KafkaTemplate<String, SpecificRecord> kafkaTemplate,
      PushNotificationEventPayloadMapper eventPayloadMapper) {
    this.pushNotificationOutboxEventRepository = pushNotificationOutboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.eventPayloadMapper = eventPayloadMapper;
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publishPendingEvents() {
    List<PushNotificationOutboxEventEntity> eventEntityList =
        pushNotificationOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING);

    for (PushNotificationOutboxEventEntity event : eventEntityList) {
      try {
        SpecificRecord payload =
            extractPayload(NotificationEventType.valueOf(event.getEventType()), event.getPayload());

        kafkaTemplate
            .send(event.getTopic(), event.getEventKey(), payload)
            .whenComplete(
                (result, ex) -> {
                  if (ex == null) {
                    onPublish(event.getId(), pushNotificationOutboxEventRepository);
                  } else {
                    onFailed(event.getId(), ex, pushNotificationOutboxEventRepository);
                  }
                });
      } catch (Exception e) {
        log.error(
            "Unable to publish push-notification outbox event: eventId={}, eventType={}",
            event.getId(),
            event.getEventType(),
            e);
        onFailed(event.getId(), e, pushNotificationOutboxEventRepository);
      }
    }
  }

  private SpecificRecord extractPayload(
      NotificationEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case PUSH_NOTIFICATION_CREATED ->
          eventPayloadMapper.toPushNotificationCreatedEventPayload(payload);
    };
  }
}
