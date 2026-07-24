package userservice.dto;

import java.util.UUID;

public record UnlockUserCommand(
        UUID authUserId
) {
}
