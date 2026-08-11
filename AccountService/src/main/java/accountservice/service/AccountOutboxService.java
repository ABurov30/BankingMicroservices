package accountservice.service;

import accountservice.entity.AccountEntity;
import accountservice.entity.AccountHoldEntity;
import accountservice.entity.AccountOutboxEventEntity;
import accountservice.repository.AccountOutboxEventRepository;
import kafkacontracts.account.AccountEventType;
import kafkacontracts.transaction.TransactionEventType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AccountOutboxService {
    private final AccountOutboxEventRepository accountOutboxEventRepository;

    public AccountOutboxService(AccountOutboxEventRepository accountOutboxEventRepository) {
        this.accountOutboxEventRepository = accountOutboxEventRepository;
    }

    public void saveAccountOutboxEvent(UUID id, AccountEventType eventType, Map<String, Object> payload) {
        saveAccountOutboxEvent(id, eventType, id + ":" + eventType.name(), payload);
    }

    public void saveAccountOutboxEvent(
            UUID id,
            AccountEventType eventType,
            String eventKey,
            Map<String, Object> payload
    ) {
        AccountOutboxEventEntity accountOutboxEventEntity = new AccountOutboxEventEntity();
        accountOutboxEventEntity.setAggregateType("ACCOUNT_TYPE");
        accountOutboxEventEntity.setAggregateId(id);
        accountOutboxEventEntity.setEventType(eventType.name());
        accountOutboxEventEntity.setTopic(eventType.getTopic());
        accountOutboxEventEntity.setEventKey(eventKey);
        accountOutboxEventEntity.setSchemaVersion(eventType.getVersion());

        accountOutboxEventEntity.setPayload(payload);

        accountOutboxEventRepository.save(accountOutboxEventEntity);
    }
}
