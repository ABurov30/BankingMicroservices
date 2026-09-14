package userservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import kafkacontracts.user.UserEventType;
import org.junit.jupiter.api.Test;

class UserOutboxEventFactoryTest {
  @Test
  void allEventTypesUseOnlyAggregateIdAsKey() {
    UUID aggregateId = UUID.randomUUID();
    for (var type : UserEventType.values()) {
      var event = UserOutboxEventFactory.create(aggregateId, type);
      assertThat(event.getEventKey()).isEqualTo(aggregateId.toString());
      assertThat(event.getAggregateId()).isEqualTo(aggregateId);
      assertThat(event.getEventType()).isEqualTo(type.name());
      assertThat(event.getTopic()).isEqualTo(type.getTopic());
      assertThat(UserOutboxEventFactory.create(UUID.randomUUID(), type).getEventKey())
          .isNotEqualTo(event.getEventKey());
    }
  }
}
