package transactionservice.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.handler.annotation.Header;
import processedevent.annotation.EventKey;
import processedevent.annotation.IdempotentKafkaEvent;

class KafkaEventIdentityTest {
  @Test
  void everyIdempotentHandlerUsesEventIdInsteadOfPartitionKey() {
    int handlers = 0;
    for (var method : TransactionKafkaListener.class.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(IdempotentKafkaEvent.class)) {
        continue;
      }
      handlers++;
      int identities = 0;
      for (var parameter : method.getParameters()) {
        if (parameter.isAnnotationPresent(EventKey.class)) {
          identities++;
          var header = parameter.getAnnotation(Header.class);
          assertThat(header).isNotNull();
          assertThat(header.value()).isEqualTo("eventId");
          assertThat(header.required()).isTrue();
        }
      }
      assertThat(identities).isEqualTo(1);
    }
    assertThat(handlers).isPositive();
  }
}
