package cardservice.dto;

import enums.common.Currency;
import java.util.UUID;

public record ReserveLimitsForTransactionCommand(
    UUID sourceCardId,
    Long minorUnits,
    UUID transactionId,
    UUID sourceAuthUserId,
    Currency currency) {}
