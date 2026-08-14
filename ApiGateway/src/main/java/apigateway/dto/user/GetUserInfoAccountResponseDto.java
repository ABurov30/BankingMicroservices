package apigateway.dto.user;

import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.AccountType;
import java.util.UUID;

public record GetUserInfoAccountResponseDto(
    UUID accountId,
    UUID ownerUserId,
    String accountNumber,
    AccountType type,
    AccountStatus status,
    AccountCurrency currency) {}
