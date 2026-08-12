package accountservice.exception;

import java.util.UUID;

public class AccountAlreadyFrozenException extends RuntimeException {
  public AccountAlreadyFrozenException(UUID accountId) {
    super("Account already frozen " + accountId);
  }
}
