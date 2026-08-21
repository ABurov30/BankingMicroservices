package apigateway.dto.account;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAccountBalanceRequestDto(
    @NotNull UUID accountId, @NotNull BigDecimal minorUnits) {}
