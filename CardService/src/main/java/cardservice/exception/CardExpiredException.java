package cardservice.exception;

import java.util.UUID;

public class CardExpiredException extends RuntimeException {
  public CardExpiredException(UUID cardId) {
    super("Card expired " + cardId);
  }
}
