package transactionservice.dto;

import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.common.Currency;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponseDto(
    UUID accountId,
    UUID ownerUserId,
    String accountNumber,
    AccountType type,
    AccountStatus status,
    BigDecimal availableBalance,
    BigDecimal reservedBalance,
    Currency currency) {}
