package apigateway.exception;

public class MissingAccessTokenException extends RuntimeException {
  public MissingAccessTokenException() {
    super("Access token not found");
  }
}
