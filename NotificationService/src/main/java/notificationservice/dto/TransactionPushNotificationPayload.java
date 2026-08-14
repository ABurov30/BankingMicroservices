package notificationservice.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransactionPushNotificationPayload(
    String accountNumber, @NotNull BigDecimal amount) {}
