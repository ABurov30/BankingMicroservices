package cardservice.exception;

import enums.common.Currency;
import java.util.UUID;

public class CardCurrencyMismatchException extends RuntimeException {
  public CardCurrencyMismatchException(
      UUID transactionId, Currency cardCurrency, Currency transactionCurrency) {
    super(
        "Card currency "
            + cardCurrency
            + " should match transaction currency "
            + transactionCurrency
            + " for transaction "
            + transactionId);
  }
}
