package authservice.service;

import authservice.entity.AuthOutboxEventEntity;
import authservice.mapper.eventpayload.AuthEventPayloadMapper;
import authservice.repository.AuthOutboxEventRepository;
import kafkacontracts.auth.AuthEventType;
import org.apache.avro.specific.SpecificRecord;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AuthOutboxPublisher implements KafkaOnSentHandler {
    private final AuthOutboxEventRepository authOutboxEventRepository;
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private final AuthEventPayloadMapper eventPayloadMapper;

    public AuthOutboxPublisher(
            AuthOutboxEventRepository authOutboxEventRepository,
            KafkaTemplate<String, SpecificRecord> kafkaTemplate,
            AuthEventPayloadMapper eventPayloadMapper
    ) {
        this.authOutboxEventRepository = authOutboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventPayloadMapper = eventPayloadMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<AuthOutboxEventEntity> eventEntityList = authOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (AuthOutboxEventEntity event : eventEntityList) {
            try {
                SpecificRecord payload = extractPayload(
                        AuthEventType.valueOf(event.getEventType()),
                        event.getPayload()
                );

                kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                onPublish(event.getId(), authOutboxEventRepository);
                            } else {
                                onFailed(event.getId(), ex, authOutboxEventRepository);
                            }
                        });
            } catch (Exception e) {
                onFailed(event.getId(), e, authOutboxEventRepository);
            }
        }
    }

    private SpecificRecord extractPayload(AuthEventType eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case AUTH_USER_CREATED -> eventPayloadMapper.toAuthUserCreatedEventPayload(payload);
            case AUTH_USER_BLOCKED -> eventPayloadMapper.toAuthUserBlockedEventPayload(payload);
            case AUTH_USER_UNLOCK -> eventPayloadMapper.toAuthUserUnlockEventPayload(payload);
            case AUTH_USER_VERIFIED -> eventPayloadMapper.toAuthUserVerifiedEventPayload(payload);
            case AUTH_USER_ROLE_CHANGED -> eventPayloadMapper.toAuthUserRoleChangedEventPayload(payload);
            case AUTH_USER_FORGET_PASSWORD -> eventPayloadMapper.toAuthUserForgetPasswordEventPayload(payload);
        };
    }
}
