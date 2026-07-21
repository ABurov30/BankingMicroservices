package accountservice.dto;

import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountCommand(
        UUID userId,
        AccountType type,
        AccountCurrency currency
) {
}
