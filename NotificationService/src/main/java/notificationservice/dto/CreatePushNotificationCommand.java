package notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import notificationservice.enums.email.EmailNotificationType;
import notificationservice.enums.push.PushNotificationType;

import java.util.UUID;

public record CreatePushNotificationCommand(
        @NotNull
        UUID authUserId,
        @NotNull
        UUID accountId,
        @NotNull
        PushNotificationType type
) {
}
