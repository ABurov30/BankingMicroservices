package apigateway.dto.auth;

import enums.auth.Roles;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record VerifyAuthUserByPrivilegeRoleRequestDto(
        @NotBlank
        UUID authUserId,
        @NotBlank
        Roles roles
) {
}
