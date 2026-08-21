package transactionservice.dto;

import enums.common.Currency;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal minorUnits,
    Currency currency,
    UUID idempotencyKey,
    UUID sourceAuthUserId,
    UUID targetAuthUserId,
    UUID sourceCardId) {}
