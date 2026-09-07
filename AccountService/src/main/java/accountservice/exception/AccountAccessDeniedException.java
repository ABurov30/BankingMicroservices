package accountservice.exception;

public class AccountAccessDeniedException extends RuntimeException {
  public AccountAccessDeniedException() {
    super("Auth user is not allowed to access account");
  }
}
