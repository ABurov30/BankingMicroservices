package cardservice.exception;

import java.util.UUID;

public class CardBlockedException extends RuntimeException {
    public CardBlockedException(UUID cardId) {
        super("Card blocked " + cardId);
    }
}
