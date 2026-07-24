package apigateway.dto.account;

import enums.account.AccountCurrency;
import enums.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAccountRequestDto(
        @NotNull
        UUID ownerUserId,
        @NotNull
        AccountType type,
        @NotNull
        AccountCurrency currency
) {
}
