package apigateway.exception;

import java.util.UUID;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(UUID accountId) {
        super("Account must be active " + accountId);
    }
}
