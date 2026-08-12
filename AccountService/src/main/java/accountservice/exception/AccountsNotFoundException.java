package accountservice.exception;

import enums.account.AccountType;
import java.util.UUID;

public class AccountsNotFoundException extends RuntimeException {
  public AccountsNotFoundException(UUID ownerUserId) {
    super("Accounts not found by ownerUserId " + ownerUserId);
  }

  public AccountsNotFoundException(AccountType accountType) {
    super("Accounts not found by type " + accountType);
  }
}
