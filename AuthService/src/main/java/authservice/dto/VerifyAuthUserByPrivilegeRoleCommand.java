package authservice.dto;

import enums.auth.Roles;

import java.util.UUID;

public record VerifyAuthUserByPrivilegeRoleCommand(
        UUID authUserId,
        Roles role
) {
}
