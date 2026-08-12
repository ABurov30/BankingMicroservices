package authservice.exception;

import java.util.UUID;

public class AuthUserNotActiveException extends RuntimeException {
  public AuthUserNotActiveException(UUID authUserId) {
    super("Auth user is not active: " + authUserId);
  }
}
