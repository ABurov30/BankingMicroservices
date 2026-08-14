package apigateway.dto.transaction;

import enums.account.AccountCurrency;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequestDto(
    @NotNull UUID sourceAccountId,
    @NotNull UUID sourceCardId,
    @NotNull UUID targetAccountId,
    @NotNull BigDecimal amount,
    @NotNull AccountCurrency currency,
    @NotNull UUID idempotencyKey) {}
