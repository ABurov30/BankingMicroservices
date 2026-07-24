package notificationservice.mapper;

import kafkacontracts.auth.AuthUserBlockedEventPayload;
import kafkacontracts.auth.AuthUserCreatedEventPayload;
import kafkacontracts.auth.AuthUserUnlockEventPayload;
import kafkacontracts.auth.AuthUserVerifiedEventPayload;
import notificationservice.dto.CreateEmailNotificationCommand;
import notificationservice.enums.NotificationType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserUnlockEventPayload payload) {
        return new CreateEmailNotificationCommand(
                payload.getEmail(),
                NotificationType.AUTH_USER_UNLOCKED,
                null
        );
    }

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserCreatedEventPayload payload) {
        return new CreateEmailNotificationCommand(
                payload.getEmail(),
                NotificationType.AUTH_USER_CREATED,
                payload.getVerificationCode()
        );
    }

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserBlockedEventPayload payload) {
        return new CreateEmailNotificationCommand(
                payload.getEmail(),
                NotificationType.AUTH_USER_BLOCKED,
                null
        );
    }

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserVerifiedEventPayload payload) {
        return new CreateEmailNotificationCommand(
                payload.getEmail(),
                NotificationType.AUTH_USER_VERIFIED,
                null
        );
    }
}
