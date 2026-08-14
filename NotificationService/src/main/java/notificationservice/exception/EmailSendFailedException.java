package notificationservice.exception;

public class EmailSendFailedException extends RuntimeException {
  public EmailSendFailedException(Throwable cause) {
    super("Failed to send email", cause);
  }
}
