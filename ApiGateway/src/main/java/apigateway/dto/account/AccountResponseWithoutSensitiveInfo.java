package apigateway.dto.account;

import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.common.Currency;
import java.util.UUID;

public record AccountResponseWithoutSensitiveInfo(
    UUID accountId, String accountNumber, AccountType type, Currency currency) {}
