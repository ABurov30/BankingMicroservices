package userservice.dto;

import java.util.UUID;

public record ChangeUserRoleCommand(
        UUID authUserId,
        String role
) {
}
