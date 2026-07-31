package notificationservice.service;

import notificationservice.document.NotificationDocument;
import notificationservice.enums.NotificationStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationScheduler {
    private final EmailSenderService emailSenderService;
    private final MongoTemplate mongoTemplate;
    private static final int BATCH_SIZE = 50;

    public NotificationScheduler(
            EmailSenderService emailSenderService,
            MongoTemplate mongoTemplate
    ) {
        this.emailSenderService = emailSenderService;
        this.mongoTemplate = mongoTemplate;
    }


    @Scheduled(fixedDelay = 5000)
    public void sendEmailNotifications() {
        for (int i = 0; i < BATCH_SIZE; i++) {
            NotificationDocument notification = getNextPendingNotification();

            if (notification == null) {
                break;
            }
            try {
                emailSenderService.send(notification);
                markSent(notification);
            } catch (Exception e) {
                markSendFailed(notification, e);
            }
        }
    }

    private NotificationDocument getNextPendingNotification() {
        Query query = new Query()
                .addCriteria(Criteria.where("status").is(NotificationStatus.PENDING))
                .addCriteria(Criteria.where("nextRetryAt").lte(LocalDateTime.now()))
                .with(Sort.by(Sort.Direction.ASC, "createdAt"));

        Update update = new Update()
                .set("status", NotificationStatus.PROCESSING);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                NotificationDocument.class
        );
    }

    private void markSent(NotificationDocument notificationDocument) {
        notificationDocument.setStatus(NotificationStatus.SENT);
        notificationDocument.setSentAt(LocalDateTime.now());
        notificationDocument.setErrorMessage(null);

        mongoTemplate.save(notificationDocument);
    }

    private void markSendFailed(NotificationDocument notificationDocument, Exception e) {
        notificationDocument.setRetryCount(notificationDocument.getRetryCount() + 1);
        notificationDocument.setErrorMessage(e.getMessage());
        notificationDocument.setSentAt(LocalDateTime.now());

        if (notificationDocument.getRetryCount() >= notificationDocument.getMaxRetryCount()) {
            notificationDocument.setStatus(NotificationStatus.FAILED);
        } else {
            notificationDocument.setStatus(NotificationStatus.PENDING);
        }

        mongoTemplate.save(notificationDocument);
    }
}
