package cardservice.exception;

public class CardGenerationFailedException extends RuntimeException {
    public CardGenerationFailedException(String message) {
        super(message);
    }
}
