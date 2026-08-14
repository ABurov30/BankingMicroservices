package cardservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReserveLimitsForTransactionCommand(
    UUID sourceCardId, BigDecimal amount, UUID transactionId, UUID sourceAuthUserId) {}
