package notificationservice.service.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import notificationservice.document.EmailNotificationDocument;
import notificationservice.enums.email.EmailNotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

class EmailNotificationSchedulerTest {
  private final EmailSenderService sender = mock(EmailSenderService.class);
  private final MongoTemplate mongo = mock(MongoTemplate.class);
  private final EmailNotificationScheduler scheduler =
      new EmailNotificationScheduler(sender, mongo);

  @Test
  void sendsPendingNotificationAndMarksItSent() {
    var notification = new EmailNotificationDocument();
    when(mongo.findAndModify(any(), any(), any(), eq(EmailNotificationDocument.class)))
        .thenReturn(notification, (EmailNotificationDocument) null);
    scheduler.sendEmailNotifications();
    assertThat(notification.getStatus()).isEqualTo(EmailNotificationStatus.SENT);
    verify(sender).send(notification);
    verify(mongo).save(notification);
  }

  @Test
  void retriesFailedNotificationThenMarksItFailedAtLimit() {
    var notification = new EmailNotificationDocument();
    notification.setRetryCount(2);
    notification.setMaxRetryCount(3);
    when(mongo.findAndModify(any(), any(), any(), eq(EmailNotificationDocument.class)))
        .thenReturn(notification, (EmailNotificationDocument) null);
    doThrow(new IllegalStateException("mail unavailable")).when(sender).send(notification);
    scheduler.sendEmailNotifications();
    assertThat(notification.getRetryCount()).isEqualTo(3);
    assertThat(notification.getStatus()).isEqualTo(EmailNotificationStatus.FAILED);
    assertThat(notification.getErrorMessage()).isEqualTo("mail unavailable");
  }
}
