package authservice.service;

import authservice.entity.AuthOutboxEventEntity;
import authservice.repository.AuthOutboxEventRepository;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class AuthOutboxPublisher implements KafkaOnSentHandler {
    private final AuthOutboxEventRepository authOutboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuthOutboxPublisher(
            AuthOutboxEventRepository authOutboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.authOutboxEventRepository = authOutboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<AuthOutboxEventEntity> eventEntityList = authOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (AuthOutboxEventEntity event : eventEntityList) {
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event.getPayload());
            } catch (JacksonException e) {
                onFailed(event.getId(), e, authOutboxEventRepository);
                continue;
            }

            kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            onPublish(event.getId(), authOutboxEventRepository);
                        } else {
                            onFailed(event.getId(), ex, authOutboxEventRepository);
                        }
                    });
        }
    }
}
