package apigateway.dto.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UpdateAccountBalanceRequestDto(
    @NotNull UUID accountId, @NotNull @Positive Long minorUnits) {}
