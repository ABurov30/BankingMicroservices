package accountservice.service;

import accountservice.entity.AccountOutboxEventEntity;
import accountservice.mapper.AccountMapper;
import accountservice.repository.AccountOutboxEventRepository;
import kafkacontracts.account.AccountEventType;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxsupport.KafkaOnSentHandler;
import outboxsupport.OutboxEventStatus;

import java.util.List;
import java.util.Map;

@Service
public class AccountOutboxPublisher implements KafkaOnSentHandler {
    private final AccountOutboxEventRepository accountOutboxEventRepository;
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private final AccountMapper accountMapper;

    public AccountOutboxPublisher(
            AccountOutboxEventRepository accountOutboxEventRepository,
            KafkaTemplate<String, SpecificRecord> kafkaTemplate,
            AccountMapper accountMapper
    ) {
        this.accountOutboxEventRepository = accountOutboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.accountMapper = accountMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<AccountOutboxEventEntity> eventEntityList = accountOutboxEventRepository.findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (AccountOutboxEventEntity event : eventEntityList) {
            try {
                SpecificRecord payload = extractPayload(
                        AccountEventType.valueOf(event.getEventType()),
                        event.getPayload()
                );

                kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                onPublish(event.getId(), accountOutboxEventRepository);
                            } else {
                                onFailed(event.getId(), ex, accountOutboxEventRepository);
                            }
                        });
            } catch (Exception e) {
                onFailed(event.getId(), e, accountOutboxEventRepository);
            }
        }
    }

    private SpecificRecord extractPayload(AccountEventType eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case ACCOUNT_CREATED -> accountMapper.toAccountCreatedEventPayload(payload);
            case ACCOUNT_FROZEN -> accountMapper.toAccountFrozenEventPayload(payload);
        };
    }
}
