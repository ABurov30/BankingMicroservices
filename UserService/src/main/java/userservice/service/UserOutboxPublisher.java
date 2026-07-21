package userservice.service;

import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import userservice.entity.UserOutboxEventEntity;
import userservice.repository.UserOutboxEventRepository;

import java.util.List;

@Service
public class UserOutboxPublisher implements KafkaOnSentHandler {
    private final UserOutboxEventRepository userOutboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserOutboxPublisher(
            UserOutboxEventRepository userOutboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.userOutboxEventRepository = userOutboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<UserOutboxEventEntity> eventEntityList = userOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (UserOutboxEventEntity event : eventEntityList) {
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event.getPayload());
            } catch (JacksonException e) {
                onFailed(event.getId(), e, userOutboxEventRepository);
                continue;
            }

            kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            onPublish(event.getId(), userOutboxEventRepository);
                        } else {
                            onFailed(event.getId(), ex, userOutboxEventRepository);
                        }
                    });
        }
    }
}
