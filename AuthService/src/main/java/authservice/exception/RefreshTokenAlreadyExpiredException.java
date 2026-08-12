package authservice.exception;

public class RefreshTokenAlreadyExpiredException extends RuntimeException {
  public RefreshTokenAlreadyExpiredException() {
    super("Refresh token already expired");
  }
}
