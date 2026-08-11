package accountservice.exception;

import java.util.UUID;

public class TransactionAlreadyProcessedException extends RuntimeException {
    public TransactionAlreadyProcessedException(UUID transactionId) {
        super("Transaction already processed " + transactionId);
    }
}
