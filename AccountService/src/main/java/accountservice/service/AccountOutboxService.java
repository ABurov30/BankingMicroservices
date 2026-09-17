package accountservice.service;

import accountservice.repository.AccountOutboxEventRepository;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.account.AccountEventType;
import kafkacontracts.cache.CacheEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountOutboxService {
  private final AccountOutboxEventRepository accountOutboxEventRepository;

  public void saveAccountOutboxEvent(
      UUID id, AccountEventType eventType, Map<String, Object> payload) {
    var accountOutboxEventEntity = AccountOutboxEventFactory.create(id, eventType);

    accountOutboxEventEntity.setPayload(payload);

    accountOutboxEventRepository.save(accountOutboxEventEntity);
  }

  public void saveCacheInvalidateEvent(
      UUID id, CacheEventType eventType, Map<String, Object> payload) {
    var accountOutboxEventEntity = AccountOutboxEventFactory.create(id, eventType);

    accountOutboxEventEntity.setPayload(payload);

    accountOutboxEventRepository.save(accountOutboxEventEntity);
  }
}
