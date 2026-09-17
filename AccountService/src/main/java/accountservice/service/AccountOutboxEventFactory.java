package accountservice.service;

import accountservice.entity.AccountOutboxEventEntity;
import java.util.UUID;
import kafkacontracts.account.AccountEventType;
import kafkacontracts.cache.CacheEventType;

public final class AccountOutboxEventFactory {
  private AccountOutboxEventFactory() {}

  public static AccountOutboxEventEntity create(UUID aggregateId, AccountEventType eventType) {
    var event = new AccountOutboxEventEntity();
    event.setAggregateType("ACCOUNT_TYPE");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }

  public static AccountOutboxEventEntity create(UUID aggregateId, CacheEventType eventType) {
    var event = new AccountOutboxEventEntity();
    event.setAggregateType("ACCOUNT_TYPE");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }
}
