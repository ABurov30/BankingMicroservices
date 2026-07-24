package accountservice.exception;

public class AccountGenerationFailedException extends RuntimeException {
    public AccountGenerationFailedException(String message) {
        super(message);
    }
}
