package notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.validation.Validator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import notificationservice.document.PushNotificationDocument;
import notificationservice.dto.CreatePushNotificationCommand;
import notificationservice.entity.PushNotificationOutboxEventEntity;
import notificationservice.enums.push.PushNotificationType;
import notificationservice.repository.EmailNotificationRepository;
import notificationservice.repository.PushNotificationOutboxEventRepository;
import notificationservice.repository.PushNotificationRepository;
import notificationservice.service.push.PushNotificationResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationOutboxKeyTest {
  @Test
  void differentNotificationsForRecipientShareKey() {
    var pushes = mock(PushNotificationRepository.class);
    var outbox = mock(PushNotificationOutboxEventRepository.class);
    var resolver = mock(PushNotificationResolver.class);
    var validator = mock(Validator.class);
    when(validator.validate(any(PushNotificationDocument.class))).thenReturn(Set.of());
    when(resolver.resolveTitle(any())).thenReturn("Title");
    when(resolver.resolveBody(any(), any())).thenReturn("Body");
    when(pushes.save(any(PushNotificationDocument.class)))
        .thenAnswer(
            invocation -> {
              PushNotificationDocument document = invocation.getArgument(0);
              document.setId(UUID.randomUUID());
              return document;
            });
    var service =
        new NotificationService(
            mock(EmailNotificationRepository.class), pushes, resolver, outbox, validator);
    UUID recipient = UUID.randomUUID();
    service.createPushNotification(
        new CreatePushNotificationCommand(Map.of(), PushNotificationType.CARD_CREATED, recipient));
    service.createPushNotification(
        new CreatePushNotificationCommand(
            Map.of(), PushNotificationType.ACCOUNT_FROZEN, recipient));
    var events = ArgumentCaptor.forClass(PushNotificationOutboxEventEntity.class);
    verify(outbox, times(2)).save(events.capture());
    assertThat(events.getAllValues())
        .allSatisfy(
            event -> {
              assertThat(event.getEventKey()).isEqualTo(recipient.toString());
              assertThat(event.getAggregateId()).isEqualTo(recipient);
              assertThat(event.getAggregateType()).isEqualTo("AUTH_USER");
            });
  }
}
