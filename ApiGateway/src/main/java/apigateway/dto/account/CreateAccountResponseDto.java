package apigateway.dto.account;

import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountResponseDto(
    UUID accountId,
    UUID ownerUserId,
    String accountNumber,
    AccountType type,
    AccountStatus status,
    BigDecimal availableBalance,
    BigDecimal reservedBalance,
    AccountCurrency currency) {}
