package transactionservice.exception;

import java.util.UUID;

public class IdempotentTransactionNotFoundException extends RuntimeException {
  public IdempotentTransactionNotFoundException(UUID idempotencyKey) {
    super("Transaction not found by idempotency key " + idempotencyKey);
  }
}
