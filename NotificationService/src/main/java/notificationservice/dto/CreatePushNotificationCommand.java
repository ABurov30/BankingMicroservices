package notificationservice.dto;

import jakarta.validation.constraints.NotNull;
import notificationservice.enums.push.PushNotificationType;

import java.util.UUID;

public record CreatePushNotificationCommand(
        Object payload,
        @NotNull
        PushNotificationType type,
        @NotNull
        UUID authUserId
) {
}
