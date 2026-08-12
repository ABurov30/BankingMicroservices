package notificationservice.service.email;

import java.time.LocalDateTime;
import notificationservice.document.EmailNotificationDocument;
import notificationservice.enums.email.EmailNotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationScheduler {
  private static final Logger log = LoggerFactory.getLogger(EmailNotificationScheduler.class);
  private final EmailSenderService emailSenderService;
  private final MongoTemplate mongoTemplate;
  private static final int BATCH_SIZE = 50;

  public EmailNotificationScheduler(
      EmailSenderService emailSenderService, MongoTemplate mongoTemplate) {
    this.emailSenderService = emailSenderService;
    this.mongoTemplate = mongoTemplate;
  }

  @Scheduled(fixedDelay = 5000)
  public void sendEmailNotifications() {
    for (int i = 0; i < BATCH_SIZE; i++) {
      EmailNotificationDocument notification = getNextPendingNotification();

      if (notification == null) {
        break;
      }
      try {
        emailSenderService.send(notification);
        markSent(notification);
      } catch (Exception e) {
        log.error(
            "Email delivery failed; scheduling retry: notificationId={}", notification.getId(), e);
        markSendFailed(notification, e);
      }
    }
  }

  private EmailNotificationDocument getNextPendingNotification() {
    Query query =
        new Query()
            .addCriteria(Criteria.where("status").is(EmailNotificationStatus.PENDING))
            .addCriteria(Criteria.where("nextRetryAt").lte(LocalDateTime.now()))
            .with(Sort.by(Sort.Direction.ASC, "createdAt"));

    Update update = new Update().set("status", EmailNotificationStatus.PROCESSING);

    return mongoTemplate.findAndModify(
        query,
        update,
        FindAndModifyOptions.options().returnNew(true),
        EmailNotificationDocument.class);
  }

  private void markSent(EmailNotificationDocument notificationDocument) {
    notificationDocument.setStatus(EmailNotificationStatus.SENT);
    notificationDocument.setSentAt(LocalDateTime.now());
    notificationDocument.setErrorMessage(null);

    mongoTemplate.save(notificationDocument);
  }

  private void markSendFailed(EmailNotificationDocument notificationDocument, Exception e) {
    notificationDocument.setRetryCount(notificationDocument.getRetryCount() + 1);
    notificationDocument.setErrorMessage(e.getMessage());
    notificationDocument.setSentAt(LocalDateTime.now());

    if (notificationDocument.getRetryCount() >= notificationDocument.getMaxRetryCount()) {
      notificationDocument.setStatus(EmailNotificationStatus.FAILED);
    } else {
      notificationDocument.setStatus(EmailNotificationStatus.PENDING);
    }

    mongoTemplate.save(notificationDocument);
  }
}
