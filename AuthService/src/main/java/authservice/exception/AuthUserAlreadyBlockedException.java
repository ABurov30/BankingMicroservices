package authservice.exception;

import java.util.UUID;

public class AuthUserAlreadyBlockedException extends RuntimeException {
  public AuthUserAlreadyBlockedException(UUID authUserId) {
    super("Auth user already blocked " + authUserId);
  }
}
