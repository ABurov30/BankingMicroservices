package authservice.service;

import authservice.entity.AuthOutboxEventEntity;
import authservice.enums.OutboxEventStatus;
import authservice.repository.AuthOutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthOutboxPublisher {
    private final AuthOutboxEventRepository authOutboxEventRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public AuthOutboxPublisher(
            AuthOutboxEventRepository authOutboxEventRepository,
            KafkaTemplate kafkaTemplate
    ) {
        this.authOutboxEventRepository = authOutboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<AuthOutboxEventEntity> eventEntityList = authOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (AuthOutboxEventEntity event : eventEntityList) {
            kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            onPublish(event.getId());
                        } else {
                            onFailed(event.getId(), ex);
                        }
                    });
        }
    }

    @Transactional
    private void onPublish(UUID eventId) {
        AuthOutboxEventEntity event = authOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.setOutboxEventStatus(OutboxEventStatus.PUBLISHED);
        event.setSentAt(LocalDateTime.now());
        event.setRetryCount(event.getRetryCount() + 1);
        authOutboxEventRepository.save(event);
    }

    @Transactional
    private void onFailed(UUID eventId, Throwable e) {
        AuthOutboxEventEntity event = authOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        int retryCount = event.getRetryCount() + 1;
        event.setErrorMessage(e.getMessage());
        event.setNextRetryAt(LocalDateTime.now().plus(Duration.ofMillis(5000)));
        event.setRetryCount(retryCount);
        if (retryCount >= 5) {
            event.setOutboxEventStatus(OutboxEventStatus.FAILED);
        } else {
            event.setOutboxEventStatus(OutboxEventStatus.PENDING);
        }

        authOutboxEventRepository.save(event);
    }
}
