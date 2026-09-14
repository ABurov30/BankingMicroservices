package notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import kafkacontracts.notification.NotificationEventType;
import org.junit.jupiter.api.Test;

class PushNotificationOutboxEventFactoryTest {
  @Test
  void allEventTypesUseOnlyAggregateIdAsKey() {
    UUID aggregateId = UUID.randomUUID();
    for (var type : NotificationEventType.values()) {
      var event = PushNotificationOutboxEventFactory.create(aggregateId, type);
      assertThat(event.getEventKey()).isEqualTo(aggregateId.toString());
      assertThat(event.getAggregateId()).isEqualTo(aggregateId);
      assertThat(event.getEventType()).isEqualTo(type.name());
      assertThat(event.getTopic()).isEqualTo(type.getTopic());
      assertThat(PushNotificationOutboxEventFactory.create(UUID.randomUUID(), type).getEventKey())
          .isNotEqualTo(event.getEventKey());
    }
  }
}
