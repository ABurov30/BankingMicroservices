package transactionservice.dto;

import enums.common.Currency;
import java.util.UUID;

public record CreateTransactionCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    Long minorUnits,
    Currency currency,
    UUID idempotencyKey,
    UUID sourceAuthUserId,
    UUID targetAuthUserId,
    UUID sourceCardId) {}
