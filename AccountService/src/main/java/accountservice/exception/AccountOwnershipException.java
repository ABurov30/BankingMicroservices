package accountservice.exception;

public class AccountOwnershipException extends RuntimeException {
    public AccountOwnershipException() {
        super("User should be owner of account");
    }
}
