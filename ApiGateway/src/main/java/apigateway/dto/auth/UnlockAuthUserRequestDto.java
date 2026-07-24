package apigateway.dto.auth;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UnlockAuthUserRequestDto(
        UUID authUserId
) {
}
