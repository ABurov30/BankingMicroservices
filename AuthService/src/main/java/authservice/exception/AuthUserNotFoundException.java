package authservice.exception;

import java.util.UUID;

public class AuthUserNotFoundException extends RuntimeException {
  public AuthUserNotFoundException(UUID authUserId) {
    super("Auth user not found by id " + authUserId);
  }
}
