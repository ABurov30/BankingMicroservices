package authservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import kafkacontracts.auth.AuthEventType;
import org.junit.jupiter.api.Test;

class AuthOutboxEventFactoryTest {
  @Test
  void allEventTypesUseOnlyAggregateIdAsKey() {
    UUID aggregateId = UUID.randomUUID();
    for (var type : AuthEventType.values()) {
      var event = AuthOutboxEventFactory.create(aggregateId, type);
      assertThat(event.getEventKey()).isEqualTo(aggregateId.toString());
      assertThat(event.getAggregateId()).isEqualTo(aggregateId);
      assertThat(event.getEventType()).isEqualTo(type.name());
      assertThat(event.getTopic()).isEqualTo(type.getTopic());
      assertThat(AuthOutboxEventFactory.create(UUID.randomUUID(), type).getEventKey())
          .isNotEqualTo(event.getEventKey());
    }
  }
}
