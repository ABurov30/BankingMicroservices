package authservice.exception;

public class InvalidOldPasswordException extends RuntimeException {
  public InvalidOldPasswordException() {
    super("Wrong old password");
  }
}
