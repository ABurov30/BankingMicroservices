package accountservice.exception;

import enums.account.AccountType;
import enums.common.Currency;
import java.util.UUID;

public class AccountAlreadyExistsException extends RuntimeException {
  public AccountAlreadyExistsException(
      UUID ownerUserId, Currency currency, AccountType accountType) {
    super(
        "Account already exists for user "
            + ownerUserId
            + " with currency "
            + currency
            + " and type "
            + accountType);
  }
}
