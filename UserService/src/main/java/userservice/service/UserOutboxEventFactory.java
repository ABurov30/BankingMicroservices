package userservice.service;

import java.util.UUID;
import kafkacontracts.user.UserEventType;
import userservice.entity.UserOutboxEventEntity;

public final class UserOutboxEventFactory {
  private UserOutboxEventFactory() {}

  public static UserOutboxEventEntity create(UUID aggregateId, UserEventType eventType) {
    var event = new UserOutboxEventEntity();
    event.setAggregateType("USER_PROFILE");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }
}
