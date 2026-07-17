package userservice.exception;

import java.util.UUID;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(UUID authUserId) {
        super("User Profile not found by auth user id " + authUserId);
    }
}
