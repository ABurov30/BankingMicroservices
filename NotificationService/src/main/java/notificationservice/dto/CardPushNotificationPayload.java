package notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CardPushNotificationPayload(
        @NotNull
        UUID accountId,
        @NotBlank
        String accountNumber,
        @NotBlank
        String cardNumber
) {
}
