package authservice.exception;

import java.util.UUID;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(UUID authUserId) {
        super("Role not found " + authUserId);
    }
}
