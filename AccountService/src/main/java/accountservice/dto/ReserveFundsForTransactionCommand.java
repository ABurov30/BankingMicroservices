package accountservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReserveFundsForTransactionCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    UUID transactionId,
    UUID sourceAuthUserId) {}
