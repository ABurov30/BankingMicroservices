package notificationservice.listener;


import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.auth.*;
import notificationservice.mapper.command.NotificationCommandMapper;
import notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaListener {
    private final NotificationService notificationService;
    private final NotificationCommandMapper commandMapper;


   public NotificationKafkaListener  (
           NotificationService notificationService,
           NotificationCommandMapper commandMapper
   ) {
       this.notificationService = notificationService;
       this.commandMapper = commandMapper;
   }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(AuthUserCreatedEventPayload payload) {
        notificationService.createEmailNotification(commandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_BLOCKED.getTopic()}"
    )
    public void handleAuthUserBlocked(AuthUserBlockedEventPayload payload) {
        notificationService.createEmailNotification(commandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_UNLOCK.getTopic()}"
    )
    public void handleAuthUserUnlock(AuthUserUnlockEventPayload payload) {
        notificationService.createEmailNotification(commandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_VERIFIED.getTopic()}"
    )
    public void handleAuthUserVerified(AuthUserVerifiedEventPayload payload) {
        notificationService.createEmailNotification(commandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_FORGET_PASSWORD.getTopic()}"
    )
    public void handleAuthUserForgetPassword(AuthUserForgetPasswordEventPayload payload) {
        notificationService.createEmailNotification(commandMapper.toCreateEmailNotificationCommand(payload));
    }
}
