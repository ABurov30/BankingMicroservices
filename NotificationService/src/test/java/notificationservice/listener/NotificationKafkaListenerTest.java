package notificationservice.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import enums.transaction.TransactionDirection;
import kafkacontracts.account.*;
import kafkacontracts.auth.*;
import kafkacontracts.card.*;
import notificationservice.mapper.command.EmailNotificationCommandMapper;
import notificationservice.mapper.command.PushNotificationCommandMapper;
import notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;

class NotificationKafkaListenerTest {
  private final NotificationService service = mock(NotificationService.class);
  private final EmailNotificationCommandMapper email = mock(EmailNotificationCommandMapper.class);
  private final PushNotificationCommandMapper push = mock(PushNotificationCommandMapper.class);
  private final NotificationKafkaListener listener =
      new NotificationKafkaListener(service, email, push);

  @Test
  void forwardsAuthEventsAndSkipsCreatedWithoutVerificationCode() {
    var created = mock(AuthUserCreatedEventPayload.class);
    when(created.getVerificationCode()).thenReturn("");
    listener.handleAuthUserCreated(created, "id");
    verifyNoInteractions(email, service);
    when(created.getVerificationCode()).thenReturn("123");
    listener.handleAuthUserCreated(created, "id");
    var blocked = mock(AuthUserStatusChangedEventPayload.class);
    when(blocked.getStatus()).thenReturn("BLOCKED");
    listener.handleAuthUserStatusChanged(blocked, "id");
    var unlocked = mock(AuthUserStatusChangedEventPayload.class);
    when(unlocked.getStatus()).thenReturn("ACTIVE");
    listener.handleAuthUserStatusChanged(unlocked, "id");
    listener.handleAuthUserVerified(mock(AuthUserVerifiedEventPayload.class), "id");
    listener.handleAuthUserForgetPassword(mock(AuthUserForgetPasswordEventPayload.class), "id");
    verify(service, times(5)).createEmailNotification(any());
  }

  @Test
  void forwardsAccountCardAndTransactionEvents() {
    listener.handleAccountCreate(mock(AccountCreatedEventPayload.class), "id");
    listener.handleAccountFrozen(mock(AccountFrozenEventPayload.class), "id");
    listener.handleAccountUnfrozen(mock(AccountUnfrozenEventPayload.class), "id");
    listener.handleCardCreated(mock(CardCreatedEventPayload.class), "id");
    listener.handleCardFrozen(mock(CardFrozenEventPayload.class), "id");
    listener.handleCardUnfrozen(mock(CardUnfrozenEventPayload.class), "id");
    listener.handleTransactionFailed(mock(TransactionFailedEventPayload.class), "id");
    listener.handleTransactionCompleted(
        mock(TransactionCompletedEventPayload.class), "id", TransactionDirection.RECIPIENT.name());
    listener.handleTransactionCompleted(mock(TransactionCompletedEventPayload.class), "id", null);
    verify(service, times(9)).createPushNotification(any());
    verify(push)
        .toCreatePushNotificationCommand(
            any(TransactionCompletedEventPayload.class), eq(TransactionDirection.RECIPIENT));
    verify(push)
        .toCreatePushNotificationCommand(any(TransactionCompletedEventPayload.class), isNull());
  }
}
