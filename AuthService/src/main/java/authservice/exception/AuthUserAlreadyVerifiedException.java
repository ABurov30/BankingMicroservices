package authservice.exception;

public class AuthUserAlreadyVerifiedException extends RuntimeException {
  public AuthUserAlreadyVerifiedException() {
    super("Auth user already verified");
  }
}
