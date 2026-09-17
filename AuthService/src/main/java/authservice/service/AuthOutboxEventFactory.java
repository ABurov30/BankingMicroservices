package authservice.service;

import authservice.entity.AuthOutboxEventEntity;
import java.util.UUID;
import kafkacontracts.auth.AuthEventType;
import kafkacontracts.cache.CacheEventType;

public final class AuthOutboxEventFactory {
  private AuthOutboxEventFactory() {}

  public static AuthOutboxEventEntity create(UUID aggregateId, AuthEventType eventType) {
    var event = new AuthOutboxEventEntity();
    event.setAggregateType("AUTH_USER");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }

  public static AuthOutboxEventEntity create(UUID aggregateId, CacheEventType eventType) {
    var event = new AuthOutboxEventEntity();
    event.setAggregateType("AUTH_USER");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }
}
