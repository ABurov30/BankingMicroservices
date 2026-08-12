package authservice.exception;

public class AuthUserNotFoundByEmailException extends RuntimeException {
  public AuthUserNotFoundByEmailException(String email) {
    super("Auth user not found by email " + email);
  }
}
