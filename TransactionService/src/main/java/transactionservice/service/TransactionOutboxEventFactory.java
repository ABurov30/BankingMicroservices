package transactionservice.service;

import java.util.UUID;
import kafkacontracts.transaction.TransactionEventType;
import transactionservice.entity.TransactionOutboxEventEntity;

public final class TransactionOutboxEventFactory {
  private TransactionOutboxEventFactory() {}

  public static TransactionOutboxEventEntity create(
      UUID aggregateId, TransactionEventType eventType) {
    var event = new TransactionOutboxEventEntity();
    event.setAggregateType("TRANSACTION_TYPE");
    event.setAggregateId(aggregateId);
    event.setEventKey(aggregateId.toString());
    event.setEventType(eventType.name());
    event.setTopic(eventType.getTopic());
    event.setSchemaVersion(eventType.getVersion());
    return event;
  }
}
