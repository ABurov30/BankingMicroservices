package authservice.exception;

import java.util.UUID;

public class AuthUserMustBeInForgetPasswordStatusException extends RuntimeException {
  public AuthUserMustBeInForgetPasswordStatusException(UUID authUserId) {
    super("Auth user must be in forget password status: " + authUserId);
  }
}
