package userservice.dto;

import java.util.UUID;

public record VerifyUserCommand(
        UUID authUserId
) {
}
