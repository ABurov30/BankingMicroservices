package apigateway.dto.transaction;

import enums.common.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreateTransactionRequestDto(
    @NotNull UUID sourceAccountId,
    @NotNull UUID sourceCardId,
    @NotNull UUID targetAccountId,
    @NotNull @Positive Long minorUnits,
    @NotNull Currency currency,
    @NotNull UUID idempotencyKey) {}
