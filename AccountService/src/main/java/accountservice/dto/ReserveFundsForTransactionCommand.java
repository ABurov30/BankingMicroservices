package accountservice.dto;

import enums.common.Currency;
import java.util.UUID;

public record ReserveFundsForTransactionCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    Long minorUnits,
    UUID transactionId,
    UUID sourceAuthUserId,
    Currency currency) {}
