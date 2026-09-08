package apigateway.dto.response.account;

import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.common.Currency;
import java.util.UUID;

public record GetRecipientResponseDto(
    UUID accountId,
    String accountNumberLast4Chars,
    AccountType type,
    AccountStatus status,
    Currency currency) {}
