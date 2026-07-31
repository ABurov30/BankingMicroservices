package authservice.exception;

import java.util.UUID;

public class AuthUserNotFoundByEmailException extends RuntimeException {
    public AuthUserNotFoundByEmailException(String email) {
        super("Auth user not found by email " + email);
    }
}