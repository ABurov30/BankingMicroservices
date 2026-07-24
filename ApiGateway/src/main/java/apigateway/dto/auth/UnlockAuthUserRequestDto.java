package apigateway.dto.auth;

import java.util.UUID;

public record UnlockAuthUserRequestDto(
        UUID authUserId
) {
}
