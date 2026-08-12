package apigateway.exception;

public class InvalidAccessTokenException extends IllegalArgumentException {
  public InvalidAccessTokenException() {
    super("Unable to parse data from access token");
  }
}
