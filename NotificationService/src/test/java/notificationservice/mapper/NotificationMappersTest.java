package notificationservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import enums.transaction.TransactionDirection;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.account.TransactionCompletedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import notificationservice.enums.email.EmailNotificationType;
import notificationservice.enums.push.PushNotificationType;
import notificationservice.mapper.command.EmailNotificationCommandMapperImpl;
import notificationservice.mapper.command.PushNotificationCommandMapperImpl;
import notificationservice.mapper.eventpayload.PushNotificationEventPayloadMapperImpl;
import org.junit.jupiter.api.Test;

class NotificationMappersTest {
  @Test
  void mapsAuthAndTransactionPayloads() {
    UUID userId = UUID.randomUUID();
    var auth =
        AuthUserCreatedEventPayload.newBuilder()
            .setAuthUserId(userId)
            .setEmail("a@b.test")
            .setFirstName("A")
            .setLastName("B")
            .setStatus("ACTIVE")
            .setRole("USER")
            .setVerificationCode("123")
            .build();
    var email = new EmailNotificationCommandMapperImpl().toCreateEmailNotificationCommand(auth);
    assertThat(email.type()).isEqualTo(EmailNotificationType.AUTH_USER_CREATED);
    assertThat(email.verificationCode()).isEqualTo("123");

    var tx =
        TransactionCompletedEventPayload.newBuilder()
            .setAuthUserId(userId)
            .setTransactionId(UUID.randomUUID())
            .setAccountNumber("ACC")
            .setAmountMinorUnits(1250)
            .setCurrency("USD")
            .build();
    var mapper = new PushNotificationCommandMapperImpl();
    assertThat(mapper.toCreatePushNotificationCommand(tx, TransactionDirection.RECIPIENT).type())
        .isEqualTo(PushNotificationType.TRANSACTION_RECEIVED);
    assertThat(mapper.toCreatePushNotificationCommand(tx, TransactionDirection.SENDER).type())
        .isEqualTo(PushNotificationType.TRANSACTION_COMPLETED);
  }

  @Test
  void mapsEventPayloadMapAndStringUuid() {
    UUID userId = UUID.randomUUID();
    var payload =
        new PushNotificationEventPayloadMapperImpl()
            .toPushNotificationCreatedEventPayload(
                Map.of("authUserId", userId.toString(), "title", "t", "body", "b", "type", "X"));
    assertThat(payload.getAuthUserId().toString()).isEqualTo(userId.toString());
    assertThat(payload.getTitle()).isEqualTo("t");
    assertThat(payload.getBody()).isEqualTo("b");
  }
}
