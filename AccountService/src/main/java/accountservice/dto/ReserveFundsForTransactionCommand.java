package accountservice.dto;

import java.util.UUID;

public record ReserveFundsForTransactionCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    Long minorUnits,
    UUID transactionId,
    UUID sourceAuthUserId) {}
