package transactionservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import kafkacontracts.transaction.TransactionEventType;
import org.junit.jupiter.api.Test;

class TransactionOutboxEventFactoryTest {
  @Test
  void allEventTypesUseOnlyAggregateIdAsKey() {
    UUID aggregateId = UUID.randomUUID();
    for (var type : TransactionEventType.values()) {
      var event = TransactionOutboxEventFactory.create(aggregateId, type);
      assertThat(event.getEventKey()).isEqualTo(aggregateId.toString());
      assertThat(event.getAggregateId()).isEqualTo(aggregateId);
      assertThat(event.getEventType()).isEqualTo(type.name());
      assertThat(event.getTopic()).isEqualTo(type.getTopic());
      assertThat(TransactionOutboxEventFactory.create(UUID.randomUUID(), type).getEventKey())
          .isNotEqualTo(event.getEventKey());
    }
  }
}
