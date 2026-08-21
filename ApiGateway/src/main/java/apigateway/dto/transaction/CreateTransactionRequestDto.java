package apigateway.dto.transaction;

import enums.common.Currency;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequestDto(
    @NotNull UUID sourceAccountId,
    @NotNull UUID sourceCardId,
    @NotNull UUID targetAccountId,
    @NotNull BigDecimal minorUnits,
    @NotNull Currency currency,
    @NotNull UUID idempotencyKey) {}
