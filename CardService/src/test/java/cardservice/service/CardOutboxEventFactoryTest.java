package cardservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import kafkacontracts.card.CardEventType;
import org.junit.jupiter.api.Test;

class CardOutboxEventFactoryTest {
  @Test
  void allEventTypesUseOnlyAggregateIdAsKey() {
    UUID aggregateId = UUID.randomUUID();
    for (var type : CardEventType.values()) {
      var event = CardOutboxEventFactory.create(aggregateId, type);
      assertThat(event.getEventKey()).isEqualTo(aggregateId.toString());
      assertThat(event.getAggregateId()).isEqualTo(aggregateId);
      assertThat(event.getEventType()).isEqualTo(type.name());
      assertThat(event.getTopic()).isEqualTo(type.getTopic());
      assertThat(CardOutboxEventFactory.create(UUID.randomUUID(), type).getEventKey())
          .isNotEqualTo(event.getEventKey());
    }
  }
}
