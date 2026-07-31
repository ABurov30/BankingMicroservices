package notificationservice.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import notificationservice.document.NotificationDocument;
import notificationservice.dto.CreateEmailNotificationCommand;
import notificationservice.enums.NotificationStatus;
import notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final Validator validator;

    public NotificationService (
            NotificationRepository notificationRepository,
            Validator validator
    ) {
        this.notificationRepository = notificationRepository;
        this.validator = validator;
    }

    public void createEmailNotification (CreateEmailNotificationCommand command) {
        NotificationDocument notification = new NotificationDocument();

        notification.setAuthUserId(command.authUserId());
        notification.setEmail(command.email());
        notification.setType(command.type());
        notification.setVerificationCode(command.verificationCode());

        Set<ConstraintViolation<NotificationDocument>> violations = validator.validate(notification);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        notificationRepository.save(notification);
    }
}
