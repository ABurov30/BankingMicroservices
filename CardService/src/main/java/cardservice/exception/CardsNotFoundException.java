package cardservice.exception;

import java.util.UUID;

public class CardsNotFoundException extends RuntimeException {
  public CardsNotFoundException(UUID accountId) {
    super("Cards not found by accountId " + accountId);
  }
}
