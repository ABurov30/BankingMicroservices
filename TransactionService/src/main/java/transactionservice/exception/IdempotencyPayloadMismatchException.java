package transactionservice.exception;

import java.util.UUID;

public class IdempotencyPayloadMismatchException extends RuntimeException {
  public IdempotencyPayloadMismatchException(UUID idempotencyKey) {
    super("Different payload with same idempotency key " + idempotencyKey);
  }
}
