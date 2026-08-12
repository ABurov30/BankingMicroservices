package userservice.service;

import java.util.List;
import java.util.Map;
import kafkacontracts.user.UserEventType;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;
import userservice.entity.UserOutboxEventEntity;
import userservice.mapper.eventpayload.UserEventPayloadMapper;
import userservice.repository.UserOutboxEventRepository;

@Service
public class UserOutboxPublisher implements KafkaOnSentHandler {
  private static final Logger log = LoggerFactory.getLogger(UserOutboxPublisher.class);
  private final UserOutboxEventRepository userOutboxEventRepository;
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final UserEventPayloadMapper eventPayloadMapper;

  public UserOutboxPublisher(
      UserOutboxEventRepository userOutboxEventRepository,
      KafkaTemplate<String, SpecificRecord> kafkaTemplate,
      UserEventPayloadMapper eventPayloadMapper) {
    this.userOutboxEventRepository = userOutboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.eventPayloadMapper = eventPayloadMapper;
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publishPendingEvents() {
    List<UserOutboxEventEntity> eventEntityList =
        userOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING);

    for (UserOutboxEventEntity event : eventEntityList) {
      try {
        SpecificRecord payload =
            extractPayload(UserEventType.valueOf(event.getEventType()), event.getPayload());

        kafkaTemplate
            .send(event.getTopic(), event.getEventKey(), payload)
            .whenComplete(
                (result, ex) -> {
                  if (ex == null) {
                    onPublish(event.getId(), userOutboxEventRepository);
                  } else {
                    onFailed(event.getId(), ex, userOutboxEventRepository);
                  }
                });
      } catch (Exception e) {
        log.error(
            "Unable to publish user outbox event: eventId={}, eventType={}",
            event.getId(),
            event.getEventType(),
            e);
        onFailed(event.getId(), e, userOutboxEventRepository);
      }
    }
  }

  private SpecificRecord extractPayload(UserEventType eventType, Map<String, Object> payload) {
    return switch (eventType) {
      case USER_PROFILE_CREATED -> eventPayloadMapper.toUserProfileCreatedEventPayload(payload);
      case USER_PROFILE_BLOCKED -> eventPayloadMapper.toUserProfileBlockedEventPayload(payload);
      case USER_PROFILE_UNLOCK -> eventPayloadMapper.toUserProfileUnlockEventPayload(payload);
    };
  }
}
