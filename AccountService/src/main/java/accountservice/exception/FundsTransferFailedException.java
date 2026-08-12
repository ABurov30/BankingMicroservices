package accountservice.exception;

import java.util.UUID;

public class FundsTransferFailedException extends RuntimeException {
  public FundsTransferFailedException(UUID transactionId, Throwable cause) {
    super("Funds transfer failed for transaction " + transactionId, cause);
  }
}
