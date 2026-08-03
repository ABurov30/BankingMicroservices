package notificationservice.listener;


import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountFrozenEventPayload;
import kafkacontracts.auth.*;
import notificationservice.mapper.command.EmailNotificationCommandMapper;
import notificationservice.mapper.command.PushNotificationCommandMapper;
import notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaListener {
    private final NotificationService notificationService;
    private final EmailNotificationCommandMapper emailCommandMapper;
    private final PushNotificationCommandMapper pushCommandMapper;


   public NotificationKafkaListener  (
           NotificationService notificationService,
           EmailNotificationCommandMapper emailCommandMapper,
           PushNotificationCommandMapper pushCommandMapper
   ) {
       this.notificationService = notificationService;
       this.emailCommandMapper = emailCommandMapper;
       this.pushCommandMapper = pushCommandMapper;
   }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(AuthUserCreatedEventPayload payload) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_BLOCKED.getTopic()}"
    )
    public void handleAuthUserBlocked(AuthUserBlockedEventPayload payload) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_UNLOCK.getTopic()}"
    )
    public void handleAuthUserUnlock(AuthUserUnlockEventPayload payload) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_VERIFIED.getTopic()}"
    )
    public void handleAuthUserVerified(AuthUserVerifiedEventPayload payload) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_FORGET_PASSWORD.getTopic()}"
    )
    public void handleAuthUserForgetPassword(AuthUserForgetPasswordEventPayload payload) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_CREATED.getTopic()}"
    )
    public void handleAccountCreate(AccountCreatedEventPayload payload) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }
}
