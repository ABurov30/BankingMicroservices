package userservice.exception;

import java.util.UUID;

public class UserProfileAlreadyBlockedException extends RuntimeException {
    public UserProfileAlreadyBlockedException(UUID authUserId) {
        super("User profile already blocked " + authUserId);
    }
}
