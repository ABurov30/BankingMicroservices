package cardservice.exception;

import java.util.UUID;

public class InsufficientMonthlyCardLimitException extends RuntimeException {
  public InsufficientMonthlyCardLimitException(UUID transactionId) {
    super("Available monthly limit should be more than amount of transaction " + transactionId);
  }
}
