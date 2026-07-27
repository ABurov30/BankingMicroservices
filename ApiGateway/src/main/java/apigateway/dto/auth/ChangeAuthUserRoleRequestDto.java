package apigateway.dto.auth;

import enums.auth.Roles;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ChangeAuthUserRoleRequestDto(
        @NotBlank
        UUID authUserId,
        @NotBlank
        Roles role
) {
}
