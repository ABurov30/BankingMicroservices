package cardservice.exception;

import java.util.UUID;

public class CardLimitHoldAlreadyExistsException extends RuntimeException {
  public CardLimitHoldAlreadyExistsException(UUID transactionId) {
    super("Limit already holds by transaction " + transactionId);
  }
}
