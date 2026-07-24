package authservice.dto;

import java.util.UUID;

public record UnlockAuthUserCommand(
        UUID authUserId
) {
}
