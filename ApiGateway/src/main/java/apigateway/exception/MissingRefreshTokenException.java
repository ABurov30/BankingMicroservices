package apigateway.exception;

public class MissingRefreshTokenException extends RuntimeException{
    public  MissingRefreshTokenException () {
        super("Refresh token not found");
    }
}
