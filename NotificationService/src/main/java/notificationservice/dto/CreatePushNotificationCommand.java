package notificationservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import notificationservice.enums.push.PushNotificationType;

public record CreatePushNotificationCommand(
    Object payload, @NotNull PushNotificationType type, @NotNull UUID authUserId) {}
