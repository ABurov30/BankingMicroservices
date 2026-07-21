package apigateway.dto.account;

import enums.account.AccountCurrency;
import enums.account.AccountType;

import java.util.UUID;

public record CreateAccountRequestDto(
        UUID ownerUserId,
        AccountType type,
        AccountCurrency currency
) {
}
