package notificationservice.listener;


import kafkacontracts.account.*;
import kafkacontracts.auth.*;
import kafkacontracts.card.CardCreatedEventPayload;
import kafkacontracts.card.CardFrozenEventPayload;
import kafkacontracts.card.CardUnfrozenEventPayload;
import enums.transaction.TransactionDirection;
import notificationservice.annotation.EventKey;
import notificationservice.annotation.IdempotentKafkaEvent;
import notificationservice.mapper.command.EmailNotificationCommandMapper;
import notificationservice.mapper.command.PushNotificationCommandMapper;
import notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaListener {
    private static final String TRANSACTION_NOTIFICATION_DIRECTION_HEADER = "transaction-notification-direction";
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

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_CREATED.getTopic()}"
    )
    public void handleAuthUserCreated(AuthUserCreatedEventPayload payload,
                                      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_BLOCKED.getTopic()}"
    )
    public void handleAuthUserBlocked(AuthUserBlockedEventPayload payload,
                                      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_UNLOCK.getTopic()}"
    )
    public void handleAuthUserUnlock(AuthUserUnlockEventPayload payload,
                                     @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_VERIFIED.getTopic()}"
    )
    public void handleAuthUserVerified(AuthUserVerifiedEventPayload payload,
                                       @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.auth.AuthEventType).AUTH_USER_FORGET_PASSWORD.getTopic()}"
    )
    public void handleAuthUserForgetPassword(AuthUserForgetPasswordEventPayload payload,
                                             @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createEmailNotification(emailCommandMapper.toCreateEmailNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_CREATED.getTopic()}"
    )
    public void handleAccountCreate(AccountCreatedEventPayload payload,
                                    @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_FROZEN.getTopic()}"
    )
    public void handleAccountFrozen(AccountFrozenEventPayload payload,
                                    @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).ACCOUNT_UNFROZEN.getTopic()}"
    )
    public void handleAccountUnfrozen(AccountUnfrozenEventPayload payload,
                                      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.card.CardEventType).CARD_CREATED.getTopic()}"
    )
    public void handleCardCreated(CardCreatedEventPayload payload,
                                  @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.card.CardEventType).CARD_FROZEN.getTopic()}"
    )
    public void handleCardFrozen(CardFrozenEventPayload payload,
                                 @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.card.CardEventType).CARD_UNFROZEN.getTopic()}"
    )
    public void handleCardUnfrozen(CardUnfrozenEventPayload payload,
                                   @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.transaction.TransactionEventType).TRANSACTION_FAILED.getTopic()}"
    )
    public void handleTransactionFailed(TransactionFailedEventPayload payload,
                                        @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
        notificationService.createPushNotification(pushCommandMapper.toCreatePushNotificationCommand(payload));
    }

    @IdempotentKafkaEvent
    @KafkaListener(
            topics = "#{T(kafkacontracts.account.AccountEventType).TRANSACTION_COMPLETED.getTopic()}"
    )
    public void handleTransactionCompleted(TransactionCompletedEventPayload payload,
                                        @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey,
                                        @Header(value = TRANSACTION_NOTIFICATION_DIRECTION_HEADER, required = false)
                                        String transactionDirection) {
        notificationService.createPushNotification(
                pushCommandMapper.toCreatePushNotificationCommand(
                        payload,
                        transactionDirection == null ? null : TransactionDirection.valueOf(transactionDirection)
                )
        );
    }
}
