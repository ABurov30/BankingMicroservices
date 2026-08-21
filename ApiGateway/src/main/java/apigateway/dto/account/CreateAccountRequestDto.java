package apigateway.dto.account;

import enums.account.AccountType;
import enums.common.Currency;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateAccountRequestDto(
    @NotNull UUID ownerUserId, @NotNull AccountType type, @NotNull Currency currency) {}
