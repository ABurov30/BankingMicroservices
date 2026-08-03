package notificationservice.mapper.command;

import kafkacontracts.auth.*;
import notificationservice.dto.CreateEmailNotificationCommand;
import notificationservice.enums.email.EmailNotificationType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmailNotificationCommandMapper {
    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserUnlockEventPayload payload) {
        return new CreateEmailNotificationCommand(null, payload.getEmail(), EmailNotificationType.AUTH_USER_UNLOCKED, null);
    }

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserCreatedEventPayload payload) {
        return new CreateEmailNotificationCommand(payload.getAuthUserId(), payload.getEmail(), EmailNotificationType.AUTH_USER_CREATED, payload.getVerificationCode());
    }

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserBlockedEventPayload payload) {
        return new CreateEmailNotificationCommand(null, payload.getEmail(), EmailNotificationType.AUTH_USER_BLOCKED, null);
    }

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserVerifiedEventPayload payload) {
        return new CreateEmailNotificationCommand(null, payload.getEmail(), EmailNotificationType.AUTH_USER_VERIFIED, null);
    }

    default CreateEmailNotificationCommand toCreateEmailNotificationCommand(AuthUserForgetPasswordEventPayload payload) {
        return new CreateEmailNotificationCommand(payload.getAuthUserId(), payload.getEmail(), EmailNotificationType.AUTH_USER_FORGET_PASSWORD, null);
    }
}
