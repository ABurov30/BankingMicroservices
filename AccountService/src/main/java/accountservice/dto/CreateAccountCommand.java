package accountservice.dto;

import enums.account.AccountType;
import enums.common.Currency;
import java.util.UUID;

public record CreateAccountCommand(
    UUID userId, UUID authUserId, AccountType type, Currency currency) {}
