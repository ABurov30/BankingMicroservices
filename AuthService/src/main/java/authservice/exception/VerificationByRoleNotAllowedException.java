package authservice.exception;

public class VerificationByRoleNotAllowedException extends RuntimeException {
  public VerificationByRoleNotAllowedException() {
    super("Only admin or manager can verify user without code");
  }
}
