package cardservice.service;

import cardservice.entity.CardOutboxEventEntity;
import java.util.UUID;
import kafkacontracts.card.CardEventType;

public final class CardOutboxEventFactory {
  private CardOutboxEventFactory() {}

  public static CardOutboxEventEntity create(UUID aggregateId, CardEventType eventType) {
    var event = new CardOutboxEventEntity();
    event.setAggregateType("CARD");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }
}
