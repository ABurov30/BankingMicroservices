package notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kafkacontracts.notification.NotificationEventType;
import notificationservice.document.EmailNotificationDocument;
import notificationservice.document.PushNotificationDocument;
import notificationservice.dto.AccountPushNotificationPayload;
import notificationservice.dto.CreateEmailNotificationCommand;
import notificationservice.dto.CreatePushNotificationCommand;
import notificationservice.dto.GetPushNotificationResult;
import notificationservice.enums.email.EmailNotificationType;
import notificationservice.enums.push.PushNotificationStatus;
import notificationservice.enums.push.PushNotificationType;
import notificationservice.repository.EmailNotificationRepository;
import notificationservice.repository.PushNotificationOutboxEventRepository;
import notificationservice.repository.PushNotificationRepository;
import notificationservice.service.push.PushNotificationResolver;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {
  private final EmailNotificationRepository emails = mock(EmailNotificationRepository.class);
  private final PushNotificationRepository pushes = mock(PushNotificationRepository.class);
  private final PushNotificationResolver resolver = mock(PushNotificationResolver.class);
  private final PushNotificationOutboxEventRepository outbox =
      mock(PushNotificationOutboxEventRepository.class);
  private final Validator validator = mock(Validator.class);
  private final NotificationService service =
      new NotificationService(emails, pushes, resolver, outbox, validator);
  private final UUID userId = UUID.randomUUID();

  @Test
  void createsValidEmailNotification() {
    when(validator.validate(any(EmailNotificationDocument.class))).thenReturn(Set.of());
    var command =
        new CreateEmailNotificationCommand(
            userId, "user@example.com", EmailNotificationType.AUTH_USER_CREATED, "code");

    service.createEmailNotification(command);

    var captor = org.mockito.ArgumentCaptor.forClass(EmailNotificationDocument.class);
    verify(emails).save(captor.capture());
    assertThat(captor.getValue().getAuthUserId()).isEqualTo(userId);
    assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    assertThat(captor.getValue().getVerificationCode()).isEqualTo("code");
  }

  @Test
  void createsPushNotificationAndOutboxEvent() {
    when(resolver.resolveTitle(PushNotificationType.ACCOUNT_CREATED)).thenReturn("created");
    when(resolver.resolveBody(any(), any())).thenReturn("body");
    when(validator.validate(any(PushNotificationDocument.class))).thenReturn(Set.of());
    var command =
        new CreatePushNotificationCommand(
            new AccountPushNotificationPayload(userId, "ACC-1"),
            PushNotificationType.ACCOUNT_CREATED,
            userId);

    service.createPushNotification(command);

    var captor = org.mockito.ArgumentCaptor.forClass(PushNotificationDocument.class);
    verify(pushes).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(PushNotificationStatus.CREATED);
    assertThat(captor.getValue().getTitle()).isEqualTo("created");
    var eventCaptor =
        org.mockito.ArgumentCaptor.forClass(
            notificationservice.entity.PushNotificationOutboxEventEntity.class);
    verify(outbox).save(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEventType())
        .isEqualTo(NotificationEventType.PUSH_NOTIFICATION_CREATED.name());
    assertThat(eventCaptor.getValue().getPayload()).containsEntry("authUserId", userId);
  }

  @Test
  void readsAndMarksPushNotifications() {
    var first = new PushNotificationDocument();
    first.setTitle("one");
    first.setBody("body");
    var second = new PushNotificationDocument();
    second.setTitle("two");
    second.setBody("body2");
    when(pushes.findByAuthUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(first, second));
    when(pushes.findByAuthUserIdAndIdIn(eq(userId), any())).thenReturn(List.of(first, second));

    List<GetPushNotificationResult> result = service.getPushNotifications(userId);
    service.markPushNotificationsAsReaded(userId, List.of(UUID.randomUUID()));

    assertThat(result).extracting(GetPushNotificationResult::title).containsExactly("one", "two");
    assertThat(first.getStatus()).isEqualTo(PushNotificationStatus.READ);
    assertThat(second.getStatus()).isEqualTo(PushNotificationStatus.READ);
    verify(pushes).saveAll(List.of(first, second));
  }
}
