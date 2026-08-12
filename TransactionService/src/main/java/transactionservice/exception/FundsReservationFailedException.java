package transactionservice.exception;

public class FundsReservationFailedException extends RuntimeException {
  public FundsReservationFailedException(String message) {
    super(message);
  }
}
