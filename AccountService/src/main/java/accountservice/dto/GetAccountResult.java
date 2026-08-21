package accountservice.dto;

import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.common.Currency;
import java.math.BigDecimal;
import java.util.UUID;

public record GetAccountResult(
    UUID accountId,
    UUID ownerUserId,
    UUID authUserId,
    String accountNumber,
    AccountType type,
    AccountStatus status,
    BigDecimal availableBalance,
    BigDecimal reservedBalance,
    Currency currency) {}
