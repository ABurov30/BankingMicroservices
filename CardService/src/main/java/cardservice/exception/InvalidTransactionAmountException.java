package cardservice.exception;

import java.util.UUID;

public class InvalidTransactionAmountException extends RuntimeException {
  public InvalidTransactionAmountException(UUID transactionId) {
    super("Amount of transaction should be positive " + transactionId);
  }
}
