package authservice.exception;

public class InvalidOrExpiredResetPasswordTokenException extends RuntimeException {
  public InvalidOrExpiredResetPasswordTokenException() {
    super("Reset password token is invalid or expired");
  }
}
