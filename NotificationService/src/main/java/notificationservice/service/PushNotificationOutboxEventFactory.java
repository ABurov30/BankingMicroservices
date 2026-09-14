package notificationservice.service;

import java.util.UUID;
import kafkacontracts.notification.NotificationEventType;
import notificationservice.entity.PushNotificationOutboxEventEntity;

public final class PushNotificationOutboxEventFactory {
  private PushNotificationOutboxEventFactory() {}

  public static PushNotificationOutboxEventEntity create(
      UUID aggregateId, NotificationEventType eventType) {
    var event = new PushNotificationOutboxEventEntity();
    event.setAggregateType("AUTH_USER");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }
}
