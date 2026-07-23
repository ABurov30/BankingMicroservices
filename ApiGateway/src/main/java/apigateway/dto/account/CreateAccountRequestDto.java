package apigateway.dto.account;

import enums.account.AccountCurrency;
import enums.account.AccountType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateAccountRequestDto(
        @NotBlank
        UUID ownerUserId,
        @NotBlank
        AccountType type,
        @NotBlank
        AccountCurrency currency
) {
}
