package cardservice.dto;

import java.util.UUID;

public record ReserveLimitsForTransactionCommand(
    UUID sourceCardId, Long minorUnits, UUID transactionId, UUID sourceAuthUserId) {}
