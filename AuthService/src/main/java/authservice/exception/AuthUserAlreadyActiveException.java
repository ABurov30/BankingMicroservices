package authservice.exception;

import java.util.UUID;

public class AuthUserAlreadyActiveException extends RuntimeException {
  public AuthUserAlreadyActiveException(UUID authUserId) {
    super("Auth user already active " + authUserId);
  }
}
