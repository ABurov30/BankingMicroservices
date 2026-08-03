package notificationservice.dto;

import jakarta.validation.constraints.NotNull;
import notificationservice.enums.push.PushNotificationType;

import java.util.UUID;

public record CreatePushNotificationCommand(
        @NotNull
        UUID authUserId,
        @NotNull
        UUID accountId,
        @NotNull
        String accountNumber,
        String cardNumber,
        @NotNull
        PushNotificationType type
) {
}
