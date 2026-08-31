package notificationservice.dto;

import enums.common.Currency;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransactionPushNotificationPayload(
    String accountNumber, @NotNull BigDecimal amount, @NotNull Currency currency) {}
