package accountservice.service;

import accountservice.entity.AccountOutboxEventEntity;
import accountservice.repository.AccountOutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class AccountOutboxPublisher implements KafkaOnSentHandler {
    private final AccountOutboxEventRepository accountOutboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AccountOutboxPublisher(
            AccountOutboxEventRepository accountOutboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.accountOutboxEventRepository = accountOutboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<AccountOutboxEventEntity> eventEntityList = accountOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (AccountOutboxEventEntity event : eventEntityList) {
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event.getPayload());
            } catch (JacksonException e) {
                onFailed(event.getId(), e, accountOutboxEventRepository);
                continue;
            }

            kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            onPublish(event.getId(), accountOutboxEventRepository);
                        } else {
                            onFailed(event.getId(), ex, accountOutboxEventRepository);
                        }
                    });
        }
    }
}
