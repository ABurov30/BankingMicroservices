package accountservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReserveFundsForTransactionCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal minorUnits,
    UUID transactionId,
    UUID sourceAuthUserId) {}
