package notificationservice.listener;


import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import kafkacontracts.auth.AuthUserVerifiedEventPayload;
import notificationservice.mapper.NotificationMapper;
import notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaListener {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;


   public NotificationKafkaListener  (
           NotificationService notificationService,
           NotificationMapper notificationMapper
   ) {
       this.notificationService = notificationService;
       this.notificationMapper = notificationMapper;
   }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(AuthUserCreatedEventPayload payload) {
        notificationService.createEmailNotification(notificationMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_BLOCKED.getTopic()}"
    )
    public void handleAuthUserBlocked(AuthUserBlockedEventPayload payload) {
        notificationService.createEmailNotification(notificationMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_UNLOCK.getTopic()}"
    )
    public void handleAuthUserUnlock(AuthUserUnlockEventPayload payload) {
        notificationService.createEmailNotification(notificationMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_VERIFIED.getTopic()}"
    )
    public void handleAuthUserVerified(AuthUserVerifiedEventPayload payload) {
        notificationService.createEmailNotification(notificationMapper.toCreateEmailNotificationCommand(payload));
    }
}
