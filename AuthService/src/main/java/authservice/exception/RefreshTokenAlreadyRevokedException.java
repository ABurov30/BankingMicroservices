package authservice.exception;

public class RefreshTokenAlreadyRevokedException extends RuntimeException {
    public RefreshTokenAlreadyRevokedException() {
        super("Refresh token already revoked");
    }
}
