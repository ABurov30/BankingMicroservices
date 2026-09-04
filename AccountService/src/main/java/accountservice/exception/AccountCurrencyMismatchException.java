package accountservice.exception;

import enums.common.Currency;
import java.util.UUID;

public class AccountCurrencyMismatchException extends RuntimeException {
  public AccountCurrencyMismatchException(
      UUID transactionId, UUID accountId, Currency accountCurrency, Currency transactionCurrency) {
    super(
        "Account currency "
            + accountCurrency
            + " should match transaction currency "
            + transactionCurrency
            + " for account "
            + accountId
            + " and transaction "
            + transactionId);
  }
}
