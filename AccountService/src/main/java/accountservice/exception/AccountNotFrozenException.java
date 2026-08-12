package accountservice.exception;

import java.util.UUID;

public class AccountNotFrozenException extends RuntimeException {
  public AccountNotFrozenException(UUID accountId) {
    super("Account not frozen " + accountId);
  }
}
