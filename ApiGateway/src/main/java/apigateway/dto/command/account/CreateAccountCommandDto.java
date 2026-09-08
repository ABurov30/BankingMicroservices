package apigateway.dto.command.account;

import enums.account.AccountType;
import enums.common.Currency;
import java.util.UUID;

public record CreateAccountCommandDto(AccountType type, Currency currency, UUID authUserId) {}
