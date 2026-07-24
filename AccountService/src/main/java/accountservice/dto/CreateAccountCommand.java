package accountservice.dto;

import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountCommand(
        UUID userId,
        UUID authUserId,
        AccountType type,
        AccountCurrency currency
) {
}
