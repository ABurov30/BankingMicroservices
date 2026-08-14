package cardservice.exception;

import java.util.UUID;

public class InsufficientDailyCardLimitException extends RuntimeException {
  public InsufficientDailyCardLimitException(UUID transactionId) {
    super("Available daily limit should be more than amount of transaction " + transactionId);
  }
}
