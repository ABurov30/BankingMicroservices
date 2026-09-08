package apigateway.dto.request.account;

import enums.account.AccountType;
import enums.common.Currency;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequestDto(@NotNull AccountType type, @NotNull Currency currency) {}
