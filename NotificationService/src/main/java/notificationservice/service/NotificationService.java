package notificationservice.service;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import kafkacontracts.notification.NotificationEventType;
import notificationservice.document.EmailNotificationDocument;
import notificationservice.document.PushNotificationDocument;
import notificationservice.dto.CreateEmailNotificationCommand;
import notificationservice.dto.CreatePushNotificationCommand;
import notificationservice.dto.GetPushNotificationResult;
import notificationservice.entity.PushNotificationOutboxEventEntity;
import notificationservice.enums.push.PushNotificationStatus;
import notificationservice.repository.EmailNotificationRepository;
import notificationservice.repository.PushNotificationOutboxEventRepository;
import notificationservice.repository.PushNotificationRepository;
import notificationservice.service.push.PushNotificationResolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {
    private final EmailNotificationRepository emailNotificationRepository;
    private final PushNotificationRepository pushNotificationRepository;
    private final PushNotificationResolver pushNotificationResolver;
    private final PushNotificationOutboxEventRepository pushNotificationOutboxEventRepository;
    private final Validator validator;

    public NotificationService (
            EmailNotificationRepository emailNotificationRepository,
            PushNotificationRepository pushNotificationRepository,
            PushNotificationResolver pushNotificationResolver,
            PushNotificationOutboxEventRepository pushNotificationOutboxEventRepository,
            Validator validator
    ) {
        this.emailNotificationRepository = emailNotificationRepository;
        this.pushNotificationRepository = pushNotificationRepository;
        this.pushNotificationResolver = pushNotificationResolver;
        this.pushNotificationOutboxEventRepository = pushNotificationOutboxEventRepository;
        this.validator = validator;
    }

    public void createEmailNotification (CreateEmailNotificationCommand command) {
        EmailNotificationDocument emailNotification = new EmailNotificationDocument();

        emailNotification.setAuthUserId(command.authUserId());
        emailNotification.setEmail(command.email());
        emailNotification.setType(command.type());
        emailNotification.setVerificationCode(command.verificationCode());

        Set<ConstraintViolation<EmailNotificationDocument>> violations = validator.validate(emailNotification);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        emailNotificationRepository.save(emailNotification);
    }

    @Transactional
    public void createPushNotification (CreatePushNotificationCommand command) {
        PushNotificationDocument pushNotification = new PushNotificationDocument();

        pushNotification.setAuthUserId(command.authUserId());
        pushNotification.setType(command.type());
        pushNotification.setStatus(PushNotificationStatus.CREATED);
        pushNotification.setTitle(pushNotificationResolver.resolveTitle(command.type()));
        pushNotification.setBody(pushNotificationResolver.resolveBody(
                command.type(),
                command.payload()
        ));

        Set<ConstraintViolation<PushNotificationDocument>> violations = validator.validate(pushNotification);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        pushNotificationRepository.save(pushNotification);
        createPushNotificationOutboxEvent(pushNotification);
    }

    private void createPushNotificationOutboxEvent (PushNotificationDocument pushNotificationDocument) {
        PushNotificationOutboxEventEntity outboxEventEntity = new PushNotificationOutboxEventEntity();
        outboxEventEntity.setAggregateType("PUSH_NOTIFICATION");
        outboxEventEntity.setAggregateId(pushNotificationDocument.getId());
        outboxEventEntity.setEventType(NotificationEventType.PUSH_NOTIFICATION_CREATED.name());
        outboxEventEntity.setTopic(NotificationEventType.PUSH_NOTIFICATION_CREATED.getTopic());
        outboxEventEntity.setEventKey(pushNotificationDocument.getId() + ":" + NotificationEventType.PUSH_NOTIFICATION_CREATED.name());
        outboxEventEntity.setSchemaVersion(NotificationEventType.PUSH_NOTIFICATION_CREATED.getVersion());

        outboxEventEntity.setPayload(Map.of(
                "authUserId", pushNotificationDocument.getAuthUserId(),
                "title", pushNotificationDocument.getTitle(),
                "body", pushNotificationDocument.getBody()
        ));

        pushNotificationOutboxEventRepository.save(outboxEventEntity);
    }

    public List<GetPushNotificationResult> getPushNotifications(UUID authUserId) {
        return pushNotificationRepository.findByAuthUserIdOrderByCreatedAtDesc(authUserId)
                .stream()
                .map(notification -> new GetPushNotificationResult(
                        notification.getTitle(),
                        notification.getBody()
                ))
                .toList();
    }

    public void markPushNotificationsAsReaded(UUID authUserId, List<UUID> ids) {
        List<PushNotificationDocument> notifications =
                pushNotificationRepository.findByAuthUserIdAndIdIn(authUserId, ids);

        notifications.forEach(notification -> notification.setStatus(PushNotificationStatus.READ));

        pushNotificationRepository.saveAll(notifications);
    }
}
