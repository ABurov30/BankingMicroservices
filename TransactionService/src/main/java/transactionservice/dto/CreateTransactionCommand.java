package transactionservice.dto;

import enums.account.AccountCurrency;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    AccountCurrency currency,
    UUID idempotencyKey,
    UUID sourceAuthUserId,
    UUID targetAuthUserId) {}
