package authservice.dto;

import java.util.UUID;

public record BlockAuthUserCommand(
        UUID authUserId
) {
}
