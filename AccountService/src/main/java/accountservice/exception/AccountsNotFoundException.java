package accountservice.exception;

import java.util.UUID;

public class AccountsNotFoundException extends RuntimeException {
    public AccountsNotFoundException(UUID ownerUserId) {
        super("Accounts not found by ownerUserId " + ownerUserId);
    }
}
