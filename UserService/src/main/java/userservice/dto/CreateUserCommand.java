package userservice.dto;

import java.util.UUID;

public record CreateUserCommand(
        UUID authUserId,
        String email,
        String firstName,
        String lastName
) {
}
