package userservice.exception;

import java.util.UUID;

public class UserProfileAlreadyActiveException extends RuntimeException {
    public UserProfileAlreadyActiveException(UUID authUserId) {
        super("User profile already active " + authUserId);
    }
}
