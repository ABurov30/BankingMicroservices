package notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AccountPushNotificationPayload(
    @NotNull UUID accountId, @NotBlank String accountNumber) {}
