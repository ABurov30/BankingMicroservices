package accountservice.exception;

import java.util.UUID;

public class AccountHoldNotFoundException extends RuntimeException {
    public AccountHoldNotFoundException(UUID accountHoldId) {
        super("Account hold not found " + accountHoldId);
    }
}
