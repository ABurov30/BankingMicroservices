package notificationservice.mapper.command;

import enums.auth.AuthUserStatus;
import kafkacontracts.auth.*;
import notificationservice.dto.CreateEmailNotificationCommand;
import notificationservice.enums.email.EmailNotificationType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmailNotificationCommandMapper {
  default CreateEmailNotificationCommand toCreateEmailNotificationCommand(
      AuthUserStatusChangedEventPayload payload) {
    EmailNotificationType type =
        AuthUserStatus.BLOCKED.name().equals(payload.getStatus())
            ? EmailNotificationType.AUTH_USER_BLOCKED
            : EmailNotificationType.AUTH_USER_UNLOCKED;
    return new CreateEmailNotificationCommand(
        payload.getAuthUserId(), payload.getEmail(), type, null);
  }

  default CreateEmailNotificationCommand toCreateEmailNotificationCommand(
      AuthUserCreatedEventPayload payload) {
    return new CreateEmailNotificationCommand(
        payload.getAuthUserId(),
        payload.getEmail(),
        EmailNotificationType.AUTH_USER_CREATED,
        payload.getVerificationCode());
  }

  default CreateEmailNotificationCommand toCreateEmailNotificationCommand(
      AuthUserVerifiedEventPayload payload) {
    return new CreateEmailNotificationCommand(
        payload.getAuthUserId(),
        payload.getEmail(),
        EmailNotificationType.AUTH_USER_VERIFIED,
        null);
  }

  default CreateEmailNotificationCommand toCreateEmailNotificationCommand(
      AuthUserForgetPasswordEventPayload payload) {
    return new CreateEmailNotificationCommand(
        payload.getAuthUserId(),
        payload.getEmail(),
        EmailNotificationType.AUTH_USER_FORGET_PASSWORD,
        payload.getResetPasswordToken());
  }
}
